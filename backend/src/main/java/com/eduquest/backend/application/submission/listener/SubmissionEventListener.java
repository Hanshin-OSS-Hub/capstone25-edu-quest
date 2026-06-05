package com.eduquest.backend.application.submission.listener;

import com.eduquest.backend.domain.identity.model.Member;
import com.eduquest.backend.domain.identity.service.MemberQueryService;
import com.eduquest.backend.domain.learning.dto.ProblemQuery;
import com.eduquest.backend.domain.learning.event.StageClearedEvent;
import com.eduquest.backend.domain.learning.model.Stage;
import com.eduquest.backend.domain.learning.service.ProblemQueryService;
import com.eduquest.backend.domain.learning.service.StageQueryService;
import com.eduquest.backend.domain.submission.model.Evaluation;
import com.eduquest.backend.domain.submission.model.Submission;
import com.eduquest.backend.domain.submission.event.SubmissionEvaluatedEvent;
import com.eduquest.backend.domain.submission.event.WrongNoteCreateRequestedEvent;
import com.eduquest.backend.domain.submission.service.EvaluationQueryService;
import com.eduquest.backend.domain.submission.service.SubmissionQueryService;
import com.eduquest.backend.domain.submission.service.WrongNoteCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubmissionEventListener {

    private final ApplicationEventPublisher eventPublisher;
    private final StageQueryService stageQueryService;
    private final MemberQueryService memberQueryService;
    private final ProblemQueryService problemQueryService;
    private final SubmissionQueryService submissionQueryService;
    private final EvaluationQueryService evaluationQueryService;
    private final WrongNoteCommandService wrongNoteCommandService;

    @Async("virtualThreadTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleSubmissionEvaluatedEvent(SubmissionEvaluatedEvent event) {

        if (Boolean.FALSE.equals(event.isCorrect())) {
            Submission submission = submissionQueryService.findSubmissionById(event.submissionId());

            eventPublisher.publishEvent(
                    WrongNoteCreateRequestedEvent.of(
                            event.submissionId(),
                            submission.getUserId(),
                            submission.getProblemId(),
                            submission.getAnswer()
                    )
            );

            log.info("오답 제출 감지. 오답노트 생성 이벤트 발행: submissionId={}, userId={}, problemId={}",
                    event.submissionId(), submission.getUserId(), submission.getProblemId());
            return;
        }

        if (!Boolean.TRUE.equals(event.isCorrect())) {
            log.info("채점 결과가 확정 정답/오답이 아니므로 후속 처리를 건너뜁니다. submissionId={}, memberId={}, isCorrect={}",
                    event.submissionId(), event.memberId(), event.isCorrect());
            return;
        }

        Submission correctSubmission = submissionQueryService.findSubmissionById(event.submissionId());
        wrongNoteCommandService.deleteByUserIdAndProblemId(correctSubmission.getUserId(), correctSubmission.getProblemId());
        log.info("정답 제출 감지. 기존 오답노트 삭제 처리: submissionId={}, userId={}, problemId={}",
                event.submissionId(), correctSubmission.getUserId(), correctSubmission.getProblemId());

        log.info("정답 제출 감지. 스테이지 클리어 체크 시작: submissionId={}, memberId={}, stageUuid={}, problemType={}",
                event.submissionId(), event.memberId(), event.stageUuid(), event.problemType());

        if (event.stageUuid() == null) {
            log.warn("stageUuid가 없어 스테이지 클리어 보상을 처리할 수 없습니다. submissionId={}", event.submissionId());
            return;
        }

        Stage stage = stageQueryService.findStageByUuid(event.stageUuid());
        List<ProblemQuery.Detail> stageProblems = problemQueryService.findAllDetailsByStageNumber(stage.getNumber());

        if (stageProblems == null || stageProblems.isEmpty()) {
            log.info("스테이지에 등록된 백엔드 문제가 없어 보상을 지급하지 않습니다. stageUuid={}, stageNumber={}", event.stageUuid(), stage.getNumber());
            return;
        }

        Set<Long> stageProblemIds = stageProblems.stream()
                .map(ProblemQuery.Detail::id)
                .collect(Collectors.toSet());

        List<Submission> userSubmissions = submissionQueryService.findSubmissionsByUserId(event.memberId());
        Map<Long, Submission> submissionsById = userSubmissions == null
                ? Collections.emptyMap()
                : userSubmissions.stream()
                        .filter(submission -> submission.getId() != null)
                        .collect(Collectors.toMap(Submission::getId, Function.identity(), (left, right) -> left));

        List<Long> submissionIds = userSubmissions == null
                ? Collections.emptyList()
                : userSubmissions.stream()
                        .map(Submission::getId)
                        .filter(id -> id != null)
                        .toList();

        Set<Long> solvedProblemIds = evaluationQueryService.findBySubmissionIds(submissionIds).stream()
                .filter(evaluation -> Boolean.TRUE.equals(evaluation.getIsCorrect()))
                .map(Evaluation::getSubmissionId)
                .map(submissionsById::get)
                .filter(submission -> submission != null)
                .map(Submission::getProblemId)
                .filter(problemId -> problemId != null)
                .collect(Collectors.toSet());

        Set<Long> unsolvedProblemIds = stageProblemIds.stream()
                .filter(problemId -> !solvedProblemIds.contains(problemId))
                .collect(Collectors.toSet());

        if (!unsolvedProblemIds.isEmpty()) {
            log.info("아직 풀지 않은 문제가 있어 보상 미지급. stageUuid={}, stageNumber={}, unsolvedProblemIds={}",
                    event.stageUuid(), stage.getNumber(), unsolvedProblemIds);
            return;
        }

        Member member = memberQueryService.findMemberById(event.memberId());
        log.info("스테이지 클리어 감지. 보상 지급 이벤트 발행: memberId={}, userUuid={}, stageUuid={}, stageNumber={}",
                member.getId(), member.getUuid(), event.stageUuid(), stage.getNumber());
        eventPublisher.publishEvent(StageClearedEvent.of(member.getUuid(), event.stageUuid()));
    }
}
