package com.eduquest.backend.infrastructure.coderunner.piston.service;

import com.eduquest.backend.common.exception.EduQuestException;
import com.eduquest.backend.domain.learning.dto.ProblemQuery;
import com.eduquest.backend.domain.learning.model.Problem;
import com.eduquest.backend.domain.learning.service.ProblemQueryService;
import com.eduquest.backend.domain.submission.dto.request.CodeEvaluateRequest;
import com.eduquest.backend.domain.submission.dto.response.CodeEvaluateResponse;
import com.eduquest.backend.domain.submission.event.SubmissionEvaluatedEvent;
import com.eduquest.backend.domain.submission.model.Submission;
import com.eduquest.backend.domain.submission.model.enums.SubmissionStatus;
import com.eduquest.backend.domain.submission.service.*;
import com.eduquest.backend.infrastructure.coderunner.exception.CodeRunnerErrorCode;
import com.eduquest.backend.infrastructure.coderunner.repository.EvaluationQueueRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PistonEvaluationWorkerService implements EvaluationWorkerService {

    @Value("${coderunner.config.language.python.version}")
    private String languageVersion;
    @Value("${coderunner.config.language.python.file}")
    private String fileName;
    @Value("${coderunner.config.limit.compilation.time}")
    private String compileTimeLimitMs;
    @Value("${coderunner.config.limit.compilation.memory}")
    private String compileTimeMemoryLimitKb;
    @Value("${coderunner.config.limit.runtime.time}")
    private String runTimeLimitMs;
    @Value("${coderunner.config.limit.runtime.memory}")
    private String runtTimeMemoryLimitKb;

    private static final String DEFAULT_LANGUAGE = "python";
    private static final int LOG_TRUNCATE_MAX = 2000;
    private static final int retryLimit = 3;

    private final EvaluationQueueRepository evaluationQueueRepository;
    private final CodeRunnerService codeRunnerService;
    private final EvaluationCommandService evaluationCommandService;
    private final SubmissionQueryService submissionQueryService;
    private final ProblemQueryService problemQueryService;
    private final SubmissionCommandService submissionCommandService;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void processSingle() {

        UUID uuidToProcess = null;
        Long submissionId = null;

        try {
            // 1) 큐에서 꺼내 처리 (비어있으면 즉시 반환)
            uuidToProcess = evaluationQueueRepository.poll();
            if (uuidToProcess == null) {
                return;
            }

            Submission submission = submissionQueryService.findSubmissionByUuid(uuidToProcess);
            submissionCommandService.updateStatus(submission.getId(), SubmissionStatus.PROCESSING);

            submissionId = submission.getId();

            // 2) 문제 정보를 가져와서 채점 혹은 비교 처리
            Problem problem = problemQueryService.findProblemById(submission.getProblemId());

            boolean isCorrect = processEvaluation(problem, submission);

            // 3) 결과 저장
            evaluationCommandService.saveEvaluation(isCorrect, submissionId);
            submissionCommandService.updateStatus(submissionId, SubmissionStatus.SUCCEEDED);

            publishEvaluatedEvent(submission, problem, isCorrect, submissionId);

        } catch (EduQuestException e) {

            if (e.getErrorCode() == CodeRunnerErrorCode.CODE_RUNNER_CLIENT_ERROR) {
                // 실패
                submissionCommandService.updateStatus(submissionId, SubmissionStatus.FAILED);
            } else if (e.getErrorCode() == CodeRunnerErrorCode.CODE_RUNNER_SERVER_ERROR) {
                // 재시도
                submissionCommandService.updateStatus(submissionId, SubmissionStatus.RETRYING);
                processWithRetry(uuidToProcess);
            }
        }

    }

    @Override
    public void processWithRetry(UUID submissionUuid) {

        Submission submission = submissionQueryService.findSubmissionByUuid(submissionUuid);
        Long submissionId = submission.getId();
        Problem problem = problemQueryService.findProblemById(submission.getProblemId());

        int retryCount = 0;

        while (retryCount < retryLimit) {

            retryCount++;
            submissionCommandService.updateRetryCount(submissionId);

            try {

                boolean isCorrect = processEvaluation(problem, submission);
                evaluationCommandService.saveEvaluation(isCorrect, submissionId);
                submissionCommandService.updateStatus(submissionId, SubmissionStatus.SUCCEEDED);

                publishEvaluatedEvent(submission, problem, isCorrect, submissionId);
                return;

            } catch (EduQuestException exception) {

                if (exception.getErrorCode() == CodeRunnerErrorCode.CODE_RUNNER_CLIENT_ERROR) {
                    submissionCommandService.updateStatus(submissionId, SubmissionStatus.FAILED);
                    return;
                }

                if (retryCount >= retryLimit) {
                    submissionCommandService.updateStatus(submissionId, SubmissionStatus.FAILED);
                    return;
                }

            } catch (Exception exception) {
                submissionCommandService.updateStatus(submissionId, SubmissionStatus.FAILED);
                return;
            }
        }

    }

    private void publishEvaluatedEvent(Submission submission, Problem problem, boolean isCorrect, Long submissionId) {
        ProblemQuery.Detail detail = problemQueryService.findProblemByUuid(problem.getUuid());
        eventPublisher.publishEvent(
                SubmissionEvaluatedEvent.of(submissionId, submission.getUserId(), isCorrect, detail.stageUuid(), detail.type())
        );
    }

    private boolean processEvaluation(Problem problem, Submission submission) {
        String type = normalizeType(problem.getType());

        log.info("[Evaluation] problemUuid={}", problem.getUuid());
        log.info("[Evaluation] type={}", problem.getType());
        log.info("[Evaluation] normalizedType={}", type);
        log.info("[Evaluation] summary={}", problem.getSummary());
        log.info("[Evaluation] expectedOutput(raw)={}", problem.getExpectedOutput());
        log.info("[Evaluation] submission.answer(raw)=\n{}", submission.getAnswer());

        // Problem authoring rule for Unity:
        // typing/basic: expectedOutput = correct source code, e.g. print("Hello")
        // ordering/final/code: expectedOutput = expected runtime stdout, e.g. 8
        if ("typing".equals(type)) {
            String expected = normalizeCodeText(problem.getExpectedOutput());
            String answer = normalizeCodeText(submission.getAnswer());
            boolean isCorrect = expected.equals(answer);

            if (looksLikeRuntimeOutputForTypingProblem(problem.getExpectedOutput(), submission.getAnswer())) {
                log.warn("[Evaluation] basic/typing 문제의 expectedOutput은 정답 코드여야 합니다. 현재 expectedOutput={}", problem.getExpectedOutput());
            }

            log.info("[Evaluation] expectedCode(normalized)={}", expected);
            log.info("[Evaluation] answerCode(normalized)={}", answer);
            log.info("[Evaluation] isCorrect={}", isCorrect);
            return isCorrect;
        }

        if ("ordering".equals(type)) {
            return evaluateOrdering(problem, submission);
        }

        if ("final".equals(type) || "code".equals(type)) {
            return evaluateByRunningCode(problem, submission);
        }

        log.warn("[Evaluation] unknown problem type={}, fallback to typing compare", problem.getType());

        String expected = normalizeCodeText(problem.getExpectedOutput());
        String answer = normalizeCodeText(submission.getAnswer());
        boolean isCorrect = expected.equals(answer);

        log.info("[Evaluation] fallback expectedCode(normalized)={}", expected);
        log.info("[Evaluation] fallback answerCode(normalized)={}", answer);
        log.info("[Evaluation] fallback isCorrect={}", isCorrect);
        return isCorrect;

    }

    private boolean evaluateOrdering(Problem problem, Submission submission) {
        // Primary ordering rule: expectedOutput = expected runtime stdout, e.g. 8.
        // Defensive fallback for Unity ordering: if the selected block order reconstructs
        // the exact answer source, accept it even when legacy expectedOutput contains code
        // or Piston is temporarily unavailable.
        try {
            boolean outputCorrect = evaluateByRunningCode(problem, submission);
            if (outputCorrect) {
                return true;
            }
        } catch (EduQuestException exception) {
            log.warn("[Evaluation] ordering Piston evaluation failed. Trying source fallback. errorCode={}", exception.getErrorCode());
        }

        boolean sourceCorrect = evaluateOrderingBySource(problem, submission);
        log.info("[Evaluation] ordering sourceFallback isCorrect={}", sourceCorrect);
        return sourceCorrect;
    }

    private boolean evaluateOrderingBySource(Problem problem, Submission submission) {
        String answer = normalizeCodeText(submission.getAnswer());
        String expectedFromBlock = normalizeCodeText(resolveOrderingExpectedSource(problem));

        log.info("[Evaluation] ordering expectedSourceFromBlock(normalized)={}", expectedFromBlock);
        log.info("[Evaluation] ordering answerSource(normalized)={}", answer);

        if (!expectedFromBlock.isBlank() && expectedFromBlock.equals(answer)) {
            return true;
        }

        if (looksLikeCode(problem.getExample())) {
            String expectedFromExample = normalizeCodeText(problem.getExample());
            boolean isCorrect = expectedFromExample.equals(answer);

            log.info("[Evaluation] ordering expectedSourceFromExample(normalized)={}", expectedFromExample);
            log.info("[Evaluation] ordering exampleCodeFallback isCorrect={}", isCorrect);

            if (isCorrect) {
                return true;
            }
        }

        if (looksLikeCode(problem.getExpectedOutput())) {
            String expectedFromExpectedOutput = normalizeCodeText(problem.getExpectedOutput());
            boolean isCorrect = expectedFromExpectedOutput.equals(answer);

            log.warn("[Evaluation] ordering 문제의 expectedOutput은 실행 결과여야 합니다. 현재 expectedOutput이 코드처럼 보여 코드 비교 fallback을 수행합니다. expectedOutput={}", problem.getExpectedOutput());
            log.info("[Evaluation] ordering expectedSourceFromExpectedOutput(normalized)={}", expectedFromExpectedOutput);
            log.info("[Evaluation] ordering expectedOutputCodeFallback isCorrect={}", isCorrect);
            return isCorrect;
        }

        return false;
    }

    private boolean evaluateByRunningCode(Problem problem, Submission submission) {
        String source = Objects.toString(submission.getAnswer(), "");

        CodeEvaluateRequest request = CodeEvaluateRequest.of(
                htmlUnescape(source),
                DEFAULT_LANGUAGE,
                languageVersion,
                fileName,
                "",
                Long.parseLong(compileTimeLimitMs),
                Long.parseLong(compileTimeMemoryLimitKb),
                Long.parseLong(runTimeLimitMs),
                Long.parseLong(runtTimeMemoryLimitKb),
                false
        );

        CodeEvaluateResponse evaluateResponse = codeRunnerService.evaluate(request);

        if (evaluateResponse == null) {
            log.warn("[Evaluation] Piston response is null");
            return false;
        }

        String stdout = Objects.toString(evaluateResponse.stdout(), "");
        String stderr = Objects.toString(evaluateResponse.stderr(), "");
        String compileStderr = Objects.toString(evaluateResponse.compileStderr(), "");

        log.info("[Evaluation] piston.stdout(raw)=\n{}", stdout);
        log.info("[Evaluation] piston.stderr(raw)=\n{}", stderr);
        log.info("[Evaluation] piston.compileStderr(raw)=\n{}", compileStderr);
        log.info("[Evaluation] piston.exitCode={}", evaluateResponse.exitCode());
        log.info("[Evaluation] piston.compileExitCode={}", evaluateResponse.compileExitCode());
        log.info("[Evaluation] piston.signal={}", evaluateResponse.signal());
        log.info("[Evaluation] piston.timedOut={}", evaluateResponse.timedOut());

        if (!stderr.isBlank() || !compileStderr.isBlank()) {
            log.warn("[Evaluation] Piston returned stderr. Marking as incorrect.");
            return false;
        }

        if (Boolean.TRUE.equals(evaluateResponse.timedOut())
                || isNonZero(evaluateResponse.exitCode())
                || isNonZero(evaluateResponse.compileExitCode())) {
            log.warn("[Evaluation] Piston execution did not finish successfully. Marking as incorrect.");
            return false;
        }

        String expected = normalizeOutput(problem.getExpectedOutput());
        String actual = normalizeOutput(stdout);
        boolean isCorrect = expected.equals(actual);

        log.info("[Evaluation] expectedOutput(normalized)={}", expected);
        log.info("[Evaluation] stdout(normalized)={}", actual);
        log.info("[Evaluation] isCorrect={}", isCorrect);

        if ("ordering".equals(normalizeType(problem.getType())) && looksLikeCode(problem.getExpectedOutput())) {
            log.warn("[Evaluation] ordering 문제의 expectedOutput은 실행 결과여야 합니다. 현재 expectedOutput={}", problem.getExpectedOutput());
        }

        return isCorrect;
    }

    private String resolveOrderingExpectedSource(Problem problem) {
        String block = problem.getBlock();
        if (block == null || block.isBlank()) {
            log.warn("[Evaluation] ordering block is blank. Cannot build expected source.");
            return "";
        }

        try {
            JsonNode root = objectMapper.readTree(block);
            JsonNode answer = root.path("answer");
            JsonNode blocks = root.path("blocks");

            if (!answer.isArray() || !blocks.isArray()) {
                log.warn("[Evaluation] ordering block must contain answer and blocks arrays. block={}", block);
                return "";
            }

            Map<Integer, String> codeByOrder = new HashMap<>();
            for (JsonNode blockNode : blocks) {
                if (!blockNode.has("order")) {
                    continue;
                }

                int order = blockNode.path("order").asInt();
                String code = htmlUnescape(blockNode.path("code").asText(""));
                codeByOrder.put(order, code);
            }

            List<String> orderedCodes = new ArrayList<>();
            for (JsonNode orderNode : answer) {
                int order = parseOrder(orderNode);
                String code = codeByOrder.get(order);

                if (code == null) {
                    log.warn("[Evaluation] ordering answer references missing block order={}. block={}", order, block);
                    return "";
                }

                orderedCodes.add(code);
            }

            String expectedSource = String.join("\n", orderedCodes);
            log.info("[Evaluation] ordering expectedSourceFromBlock(raw)=\n{}", expectedSource);
            return expectedSource;
        } catch (Exception exception) {
            log.warn("[Evaluation] failed to parse ordering block. block={}", block, exception);
            return "";
        }
    }

    private int parseOrder(JsonNode orderNode) {
        if (orderNode.isNumber()) {
            return orderNode.asInt();
        }

        return Integer.parseInt(orderNode.asText());
    }

    private String normalizeType(String type) {
        String t = Objects.toString(type, "").trim().toLowerCase();

        if ("basic".equals(t)) return "typing";
        if ("typing".equals(t)) return "typing";
        if ("ordering".equals(t)) return "ordering";
        if ("final".equals(t)) return "final";
        if ("code".equals(t)) return "code";

        return t;
    }

    private String normalizeCodeText(String value) {
        return htmlUnescape(value)
                .replace("\r", "")
                .trim()
                .replaceAll("\\s+", "")
                .replace("\"", "'");
    }

    private String normalizeOutput(String value) {
        return htmlUnescape(value)
                .replace("\r", "")
                .trim();
    }

    private boolean looksLikeRuntimeOutputForTypingProblem(String expectedOutput, String answer) {
        String normalizedExpectedOutput = normalizeCodeText(expectedOutput);
        String normalizedAnswer = normalizeCodeText(answer);

        return normalizedAnswer.contains("(")
                && !normalizedExpectedOutput.contains("(")
                && !normalizedExpectedOutput.contains("=")
                && !normalizedExpectedOutput.contains(":");
    }

    private boolean looksLikeCode(String value) {
        String v = Objects.toString(value, "").trim();
        return v.contains("print(")
                || v.contains("=")
                || v.contains("\n")
                || v.startsWith("if ")
                || v.startsWith("for ")
                || v.startsWith("while ");
    }

    private boolean isNonZero(Integer value) {
        return value != null && value != 0;
    }

    private String htmlUnescape(String value) {
        return HtmlUtils.htmlUnescape(value == null ? "" : value);
    }
}
