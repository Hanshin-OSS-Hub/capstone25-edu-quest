package com.eduquest.backend.application.submission.listener;

import com.eduquest.backend.domain.submission.event.WrongNoteCreateRequestedEvent;
import com.eduquest.backend.domain.submission.service.WrongNoteCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class WrongNoteCreateEventListener {

	private final WrongNoteCommandService wrongNoteCommandService;

	@Async("virtualThreadTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void handleWrongNoteCreateRequested(WrongNoteCreateRequestedEvent event) {
		log.info("오답노트 생성 이벤트 수신: submissionId={}, userId={}, problemId={}",
				event.submissionId(), event.memberId(), event.problemId());

		if (event.memberId() == null || event.problemId() == null || event.wrongAnswer() == null) {
			log.warn("오답노트 생성 이벤트 데이터가 부족해 처리를 건너뜁니다. submissionId={}, userId={}, problemId={}, wrongAnswerIsNull={}",
					event.submissionId(), event.memberId(), event.problemId(), event.wrongAnswer() == null);
			return;
		}

		Long wrongNoteId = wrongNoteCommandService.createWrongNote(event.wrongAnswer(), event.memberId(), event.problemId());
		log.info("오답노트 생성/업데이트 완료: wrongNoteId={}, submissionId={}", wrongNoteId, event.submissionId());
	}
}

