package com.eduquest.backend.application.submission.service;

import com.eduquest.backend.application.submission.exception.SubMissionErrorCode;
import com.eduquest.backend.common.exception.EduQuestException;
import com.eduquest.backend.domain.identity.service.MemberQueryService;
import com.eduquest.backend.domain.learning.dto.ProblemQuery;
import com.eduquest.backend.domain.learning.service.ProblemQueryService;
import com.eduquest.backend.domain.submission.event.EvaluationReadyEvent;
import com.eduquest.backend.domain.submission.model.Submission;
import com.eduquest.backend.domain.submission.service.SubmissionCommandService;
import com.eduquest.backend.domain.submission.service.SubmissionQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionService {

	private final ProblemQueryService problemQueryService;
	private final MemberQueryService memberQueryService;

	private final SubmissionCommandService submissionCommandService;
	private final SubmissionQueryService submissionQueryService;
	private final ApplicationEventPublisher eventPublisher;


	@Transactional
	public UUID submit(UUID problemUuid, String userId, String answer) {
		if (problemUuid == null) {
			throw new EduQuestException(SubMissionErrorCode.INVALID_SUBMISSION_REQUEST, Map.of("problemUuid", "문제 UUID는 필수입니다."));
		}
		if (userId == null || userId.isBlank()) {
			throw new EduQuestException(SubMissionErrorCode.UNAUTHENTICATED_SUBMISSION, Map.of("userId", "인증된 사용자 정보가 없습니다."));
		}
		if (answer == null || answer.isBlank()) {
			throw new EduQuestException(SubMissionErrorCode.INVALID_SUBMISSION_REQUEST, Map.of("answer", "답안은 필수입니다."));
		}

		ProblemQuery.Detail detail = problemQueryService.findProblemByUuid(problemUuid);
		Long problemId = detail.id();
		log.info("Problem lookup succeeded for submission: problemUuid={}, problemId={}", problemUuid, problemId);

		Long memberId = memberQueryService.findMemberIdByUserId(userId);
		log.info("Member lookup succeeded for submission: userId={}, memberId={}", userId, memberId);

		// domain 모델 생성 후 도메인 포트로 저장
		Submission submissionDomain = Submission.of(memberId, problemId, answer);
		Long submissionId = submissionCommandService.saveSubmission(submissionDomain);
		log.info("Submission saved: submissionId={}", submissionId);

		// 저장된 Submission을 조회하여 UUID를 획득
		Submission savedSubmission = submissionQueryService.findSubmissionById(submissionId);
		UUID submissionUuid = savedSubmission.getUuid();
		log.info("Submission uuid loaded: submissionId={}, submissionUuid={}", submissionId, submissionUuid);

		// 평가 요청 이벤트 발행 (ApplicationEventPublisher 사용)
		eventPublisher.publishEvent(EvaluationReadyEvent.of(this, submissionUuid));

		log.info("Submission stored and evaluation enqueued: submissionId={}, submissionUuid={}, memberId={}", submissionId, submissionUuid, memberId);

		return submissionUuid;

	}

}

