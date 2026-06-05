package com.eduquest.backend.application.submission.service;

import com.eduquest.backend.application.submission.dto.EvaluationInfo;
import com.eduquest.backend.application.submission.exception.SubMissionErrorCode;
import com.eduquest.backend.common.exception.EduQuestException;
import com.eduquest.backend.domain.identity.service.MemberQueryService;
import com.eduquest.backend.domain.submission.model.Evaluation;
import com.eduquest.backend.domain.submission.model.Submission;
import com.eduquest.backend.domain.submission.model.enums.SubmissionStatus;
import com.eduquest.backend.domain.submission.service.EvaluationQueryService;
import com.eduquest.backend.domain.submission.service.SubmissionQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluationService {

    private final SubmissionQueryService submissionQueryService;
    private final MemberQueryService memberQueryService;
    private final EvaluationQueryService evaluationQueryService;

    @Transactional(readOnly = true)
    public EvaluationInfo findBySubmissionUuid(UUID submissionUuid, String userId) {

        Long memberId = memberQueryService.findMemberIdByUserId(userId);

        Submission submission = submissionQueryService.findSubmissionByUuid(submissionUuid);
        SubmissionStatus submissionStatus = submissionQueryService.findSubmissionStatusBySubmissionId(submission.getId());

        if (!submission.getUserId().equals(memberId)) {
            throw new EduQuestException(SubMissionErrorCode.FORBIDDEN_SUBMISSION_ACCESS);
        }

        switch (submissionStatus) {
            case PENDING, PROCESSING, RETRYING: {
                log.info("[EvaluationPolling] submissionUuid={}, status={}, response=pending", submissionUuid, submissionStatus);
                return EvaluationInfo.pendingInfo();
            }
            case FAILED: {
                log.warn("[EvaluationPolling] submissionUuid={}, status=FAILED, response=completed false", submissionUuid);
                return EvaluationInfo.of(false, null);
            }
            case SUCCEEDED: break;
        }

        Evaluation evaluation = evaluationQueryService.findBySubmissionId(submission.getId());

        log.info("[EvaluationPolling] submissionUuid={}, status=SUCCEEDED, isCorrect={}", submissionUuid, evaluation.getIsCorrect());
        return EvaluationInfo.of(evaluation.getIsCorrect(), evaluation.getCreatedAt());

    }
}
