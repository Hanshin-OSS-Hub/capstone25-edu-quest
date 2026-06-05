package com.eduquest.backend.application.learning.service;

import com.eduquest.backend.application.learning.dto.HintDto;
import com.eduquest.backend.application.learning.dto.ProblemCommand;
import com.eduquest.backend.application.learning.dto.ProblemDto;
import com.eduquest.backend.application.learning.dto.ProblemListDto;
import com.eduquest.backend.application.learning.exception.LearningErrorCode;
import com.eduquest.backend.common.exception.EduQuestException;
import com.eduquest.backend.domain.identity.dto.MemberQuery;
import com.eduquest.backend.domain.identity.service.MemberQueryService;
import com.eduquest.backend.domain.learning.dto.ProblemQuery;
import com.eduquest.backend.domain.learning.model.Hint;
import com.eduquest.backend.domain.learning.model.HintHistory;
import com.eduquest.backend.domain.learning.model.Problem;
import com.eduquest.backend.domain.learning.service.HintHistoryCommandService;
import com.eduquest.backend.domain.learning.service.HintHistoryQueryService;
import com.eduquest.backend.domain.learning.service.ProblemCommandService;
import com.eduquest.backend.domain.learning.service.ProblemQueryService;
import com.eduquest.backend.domain.learning.service.StageQueryService;
import com.eduquest.backend.domain.reward.service.WalletCommandService;
import com.eduquest.backend.domain.reward.service.WalletQueryService;
import com.eduquest.backend.domain.submission.model.Evaluation;
import com.eduquest.backend.domain.submission.model.Submission;
import com.eduquest.backend.domain.submission.service.EvaluationQueryService;
import com.eduquest.backend.domain.submission.service.SubmissionQueryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProblemService {

    private static final Set<String> ALLOWED_PROBLEM_TYPES = Set.of("basic", "typing", "ordering", "final", "code");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final StageQueryService stageQueryService;
    private final ProblemCommandService problemCommandService;
    private final ProblemQueryService problemQueryService;
    private final HintHistoryCommandService hintHistoryCommandService;
    private final HintHistoryQueryService hintHistoryQueryService;
    private final MemberQueryService  memberQueryService;
    private final WalletCommandService walletCommandService;
    private final WalletQueryService walletQueryService;
    private final SubmissionQueryService submissionQueryService;
    private final EvaluationQueryService evaluationQueryService;

    public void createProblem(ProblemCommand command) {
        String problemType = normalizeProblemType(command.type());
        validateOrderingBlock(problemType, command.block());
        log.info(
                "Problem create command: type={}, summary={}, expectedOutput={}, blockNull={}, block={}",
                problemType,
                command.summary(),
                command.expectedOutput(),
                command.block() == null,
                command.block()
        );

        List<Hint> domainHints = command.hints() == null ? List.of() : command.hints().stream()
                .map(h -> Hint.of(h.level(), h.point(), h.content()))
                .collect(Collectors.toList());

        UUID stageUuid = UUID.fromString(command.stageUuid());
        Long stageId = stageQueryService.findIdByUuid(stageUuid);

        Problem problem = Problem.of(stageId, problemType, command.number(), command.summary(), command.example(), command.expectedOutput(), command.block(), domainHints);

        Long problemId = problemCommandService.saveProblem(problem);
        log.info("Problem create saved: problemId={}, type={}", problemId, problem.getType());

    }

    public void updateProblem(UUID uuid, ProblemCommand command) {
        String problemType = normalizeProblemType(command.type());
        validateOrderingBlock(problemType, command.block());
        log.info(
                "Problem update command received: type={}, summary={}, expectedOutput={}, blockNull={}, block={}",
                problemType,
                command.summary(),
                command.expectedOutput(),
                command.block() == null,
                command.block()
        );

        ProblemQuery.Detail detail = problemQueryService.findProblemByUuid(uuid);
        log.info("Problem update lookup succeeded: problemId={}, problemUuid={}", detail.id(), detail.uuid());

        List<Hint> domainHints = command.hints() == null ? List.of() : command.hints().stream()
                .map(h -> Hint.of(h.level(), h.point(), h.content()))
                .collect(Collectors.toList());
        log.info("Problem update hints mapped: hintCount={}", domainHints.size());

        UUID stageUuid = UUID.fromString(command.stageUuid());
        log.info("Problem update stageUuid parsed: stageUuid={}", stageUuid);

        Long stageId = stageQueryService.findIdByUuid(stageUuid);
        log.info("Problem update stage lookup succeeded: stageId={}", stageId);

        Problem problem = Problem.of(detail.uuid(), detail.id(), stageId, problemType, command.number(), command.summary(), command.example(), command.expectedOutput(), command.block(), domainHints);

        problem.updateProblem(stageId, problemType, command.number(), command.summary(), command.example(), command.expectedOutput(), command.block());
        problem.updateHints(domainHints);

        log.info("Problem update command executing: problemId={}, problemUuid={}", problem.getId(), problem.getUuid());
        Long updatedProblemId = problemCommandService.updateProblem(problem);
        log.info("Problem update saved: problemId={}, type={}", updatedProblemId, problem.getType());

    }

    public void deleteProblem(UUID uuid) {
        problemCommandService.deleteProblem(uuid);
    }

    public ProblemDto getProblem(UUID uuid) {

        ProblemQuery.Detail detail = problemQueryService.findProblemByUuid(uuid);

        List<HintDto> hintList = detail.hints() == null ? List.of() : detail.hints().stream()
                .map(h -> HintDto.of(h.level(), h.point(), h.content()))
                .collect(Collectors.toList());

        return ProblemDto.of(
                detail.uuid(),
                detail.stageUuid(),
                detail.stageTitle(),
                detail.stageNumber(),
                detail.type(),
                detail.number(),
                detail.summary(),
                detail.example(),
                detail.expectedOutput(),
                detail.block(),
                hintList
        );
    }

    public List<ProblemDto> findProblemsByStageNumber(Integer stageNumber) {
        List<ProblemQuery.Detail> details = problemQueryService.findAllDetailsByStageNumber(stageNumber);

        if (details == null || details.isEmpty()) {
            return List.of();
        }

        return details.stream().map(detail -> {
            List<HintDto> hintList = detail.hints() == null ? List.of() : detail.hints().stream()
                    .map(h -> HintDto.of(h.level(), h.point(), h.content()))
                    .collect(Collectors.toList());

            return ProblemDto.of(
                    detail.uuid(),
                    detail.stageUuid(),
                    detail.stageTitle(),
                    detail.stageNumber(),
                    detail.type(),
                    detail.number(),
                    detail.summary(),
                    detail.example(),
                    detail.expectedOutput(),
                    detail.block(),
                    hintList
            );
        }).collect(Collectors.toList());
    }

    public ProblemListDto listProblems(int page, int size, String sort, Boolean isAsc) {
        List<ProblemQuery.Detail> details = problemQueryService.findDetailsByPagination(page, size, sort, isAsc);

        if (details == null || details.isEmpty()) {
            return ProblemListDto.of(page, size, sort, isAsc, List.of());
        }

        List<ProblemDto> results = details.stream().map(detail -> {
            List<HintDto> hintList = detail.hints() == null ? List.of() : detail.hints().stream()
                    .map(h -> HintDto.of(h.level(), h.point(), h.content()))
                    .collect(Collectors.toList());

            return ProblemDto.of(
                    detail.uuid(),
                    detail.stageUuid(),
                    detail.stageTitle(),
                    detail.stageNumber(),
                    detail.type(),
                    detail.number(),
                    detail.summary(),
                    detail.example(),
                    detail.expectedOutput(),
                    detail.block(),
                    hintList
            );
        }).collect(Collectors.toList());

        return ProblemListDto.of(page, size, sort, isAsc, results);
    }

    @Transactional(readOnly = true)
    public ProblemListDto findReviewProblemsByUserUuid(UUID userUuid, String requesterUserId) {
        MemberQuery.UserProfile userProfile = memberQueryService.findUserProfileByUuid(userUuid);
        log.info("복습 문제 조회 시작: userUuid={}, userId={}, requesterUserId={}",
                userUuid, userProfile.userId(), requesterUserId);

        Long memberId = memberQueryService.findMemberIdByUuid(userUuid);
        List<Submission> submissions = submissionQueryService.findSubmissionsByUserId(memberId);

        if (submissions == null || submissions.isEmpty()) {
            log.info("복습 문제 조회 결과 없음: 제출 내역이 없습니다. memberId={}", memberId);
            return ProblemListDto.of(0, 0, "created_at", false, List.of());
        }

        Map<Long, Long> submissionToProblemMap = submissions.stream()
                .filter(submission -> submission.getId() != null && submission.getProblemId() != null)
                .collect(Collectors.toMap(Submission::getId, Submission::getProblemId, (left, right) -> left));

        if (submissionToProblemMap.isEmpty()) {
            log.info("복습 문제 조회 결과 없음: 유효한 제출-문제 연결이 없습니다. memberId={}", memberId);
            return ProblemListDto.of(0, 0, "created_at", false, List.of());
        }

        Set<Long> solvedProblemIds = evaluationQueryService.findBySubmissionIds(submissionToProblemMap.keySet().stream().toList()).stream()
                .filter(evaluation -> Boolean.TRUE.equals(evaluation.getIsCorrect()))
                .map(Evaluation::getSubmissionId)
                .map(submissionToProblemMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (solvedProblemIds.isEmpty()) {
            log.info("복습 문제 조회 결과 없음: 정답 처리된 문제가 없습니다. memberId={}", memberId);
            return ProblemListDto.of(0, 0, "created_at", false, List.of());
        }

        List<ProblemDto> results = problemQueryService.findDetailsByProblemIds(solvedProblemIds.stream().toList()).stream()
                .map(detail -> {
                    List<HintDto> hintList = detail.hints() == null ? List.of() : detail.hints().stream()
                            .map(h -> HintDto.of(h.level(), h.point(), h.content()))
                            .collect(Collectors.toList());

                    return ProblemDto.of(
                            detail.uuid(),
                            detail.stageUuid(),
                            detail.stageTitle(),
                            detail.stageNumber(),
                            detail.type(),
                            detail.number(),
                            detail.summary(),
                            detail.example(),
                            detail.expectedOutput(),
                            detail.block(),
                            hintList
                    );
                })
                .toList();

        log.info("복습 문제 조회 완료: memberId={}, solvedProblemCount={}, resultCount={}",
                memberId, solvedProblemIds.size(), results.size());

        return ProblemListDto.of(0, results.size(), "number", true, results);
    }

    @Transactional
    public HintDto findHint(UUID problemUuid, Integer level, String userId) {

        ProblemQuery.HintDetail hintDetail = problemQueryService.findHintByProblemUuidAndLevel(problemUuid, level);
        Long memberId = memberQueryService.findMemberIdByUserId(userId);
        UUID memberUuid = memberQueryService.findMemberUuidByUserId(userId);
        MemberQuery.UserProfile userProfile = memberQueryService.findUserProfileByUuid(memberUuid);

        HintDto hintDto = HintDto.of(
                hintDetail.level(),
                hintDetail.point(),
                hintDetail.content()
        );

        if (isAdminRole(userProfile.role())) {
            log.info("관리자 힌트 무료 사용: memberId={}, problemUuid={}, level={}", memberId, problemUuid, level);
            return hintDto;
        }

        boolean isHintHistoryExists = hintHistoryQueryService.isHintHistoryExistsByHintIdAndMemberId(hintDetail.id(), memberId);
        long hintPoint = hintDto.point() == null ? 0L : hintDto.point();

        if (isHintHistoryExists) {
            log.info("이미 사용한 힌트라 코인을 차감하지 않습니다. memberId={}, problemId={}, hintId={}, level={}",
                    memberId, hintDetail.problemId(), hintDetail.id(), hintDto.level());
            return hintDto;
        }

        Long balance = walletQueryService.findByUserId(memberId).getBalance();
        if (balance == null || balance < hintPoint) {
            log.info("힌트 사용 실패: 포인트 잔액 부족. memberId={}, problemId={}, hintId={}, level={}, balance={}, cost={}",
                    memberId, hintDetail.problemId(), hintDetail.id(), hintDto.level(), balance, hintPoint);
            throw new EduQuestException(LearningErrorCode.INSUFFICIENT_BALANCE);
        }

        if (hintPoint > 0) {
            walletCommandService.changeBalance(
                    memberId,
                    -hintPoint,
                    "힌트 사용 - 문제번호 : " + hintDetail.problemId() + " - 힌트 레벨 " + hintDto.level()
            );
        }

        hintHistoryCommandService.saveHintHistory(HintHistory.of(hintDetail.id(), memberId));
        log.info("힌트 사용 완료. memberId={}, problemId={}, hintId={}, level={}, cost={}",
                memberId, hintDetail.problemId(), hintDetail.id(), hintDto.level(), hintPoint);

        return hintDto;

    }

    private boolean isAdminRole(String role) {
        if (role == null) {
            return false;
        }

        String normalizedRole = role.trim().toUpperCase(Locale.ROOT);
        return "ADMIN".equals(normalizedRole) || "ROLE_ADMIN".equals(normalizedRole);
    }

    private String normalizeProblemType(String type) {
        String normalizedType = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_PROBLEM_TYPES.contains(normalizedType)) {
            throw new EduQuestException(
                    LearningErrorCode.VALIDATION_ERROR,
                    Map.of("type", "허용되는 문제 타입은 basic, typing, ordering, final 입니다.")
            );
        }

        return normalizedType;
    }

    private void validateOrderingBlock(String problemType, String block) {
        if (!"ordering".equals(problemType)) {
            return;
        }
        if (block == null || block.isBlank()) {
            throw new EduQuestException(
                    LearningErrorCode.VALIDATION_ERROR,
                    Map.of("block", "ordering 문제는 block.answer와 block.blocks가 필요합니다.")
            );
        }

        try {
            JsonNode root = OBJECT_MAPPER.readTree(block);
            JsonNode answer = root.get("answer");
            JsonNode blocks = root.get("blocks");

            if (answer == null || !answer.isArray() || answer.isEmpty()
                    || blocks == null || !blocks.isArray() || blocks.isEmpty()) {
                throw new EduQuestException(
                        LearningErrorCode.VALIDATION_ERROR,
                        Map.of("block", "ordering 문제는 비어 있지 않은 block.answer와 block.blocks가 필요합니다.")
                );
            }

            Set<Integer> blockOrders = new java.util.HashSet<>();
            for (JsonNode blockNode : blocks) {
                JsonNode orderNode = blockNode.get("order");
                if (orderNode == null || !orderNode.canConvertToInt()) {
                    throw new EduQuestException(
                            LearningErrorCode.VALIDATION_ERROR,
                            Map.of("block.blocks", "각 코드 블록에는 숫자 order가 필요합니다.")
                    );
                }
                blockOrders.add(orderNode.asInt());
            }

            for (JsonNode answerNode : answer) {
                if (!answerNode.canConvertToInt() || !blockOrders.contains(answerNode.asInt())) {
                    throw new EduQuestException(
                            LearningErrorCode.VALIDATION_ERROR,
                            Map.of("block.answer", "answer 배열의 값은 blocks의 order와 일치해야 합니다.")
                    );
                }
            }
        } catch (EduQuestException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new EduQuestException(
                    LearningErrorCode.VALIDATION_ERROR,
                    Map.of("block", "ordering 문제의 block은 유효한 JSON이어야 합니다.")
            );
        }
    }

}
