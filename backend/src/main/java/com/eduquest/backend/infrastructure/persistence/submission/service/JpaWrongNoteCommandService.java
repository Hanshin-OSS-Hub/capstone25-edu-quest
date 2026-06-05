package com.eduquest.backend.infrastructure.persistence.submission.service;

import com.eduquest.backend.common.exception.EduQuestException;
import com.eduquest.backend.domain.submission.model.WrongNote;
import com.eduquest.backend.domain.submission.service.WrongNoteCommandService;
import com.eduquest.backend.infrastructure.persistence.submission.entity.WrongNoteEntity;
import com.eduquest.backend.infrastructure.persistence.submission.exception.SubmissionDatabaseErrorCode;
import com.eduquest.backend.infrastructure.persistence.submission.mapper.WrongNoteEntityMapper;
import com.eduquest.backend.infrastructure.persistence.submission.repository.WrongNoteJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JpaWrongNoteCommandService implements WrongNoteCommandService {

    private final WrongNoteJpaRepository wrongNoteJpaRepository;
    private final WrongNoteEntityMapper mapper;

    @Override
    @Transactional
    public Long saveOrUpdateWrongNote(WrongNote wrongNote) {
        if (wrongNote == null) throw new IllegalArgumentException("wrongNote must not be null");

        Optional<WrongNoteEntity> existing = findPrimaryWrongNote(wrongNote.getUserId(), wrongNote.getProblemId());

        if (existing.isPresent()) {
            WrongNoteEntity entity = existing.get();
            entity.updateWrongAnswer(wrongNote.getWrongAnswer(), wrongNote.getAiExplanation(), wrongNote.getNextReviewAt());
            if (Boolean.TRUE.equals(wrongNote.getIsReviewed())) {
                entity.markReviewed();
            }
            return wrongNoteJpaRepository.save(entity).getId();
        } else {
            WrongNoteEntity newEntity = mapper.toEntity(wrongNote);
            return wrongNoteJpaRepository.save(newEntity).getId();
        }
    }

    @Override
    @Transactional
    public Long createWrongNote(String wrongAnswer, Long userId, Long problemId) {
        if (userId == null || problemId == null) throw new IllegalArgumentException("userId and problemId must not be null");

        Optional<WrongNoteEntity> existing = findPrimaryWrongNote(userId, problemId);

        if (existing.isPresent()) {
            WrongNoteEntity entity = existing.get();
            entity.updateWrongAnswer(wrongAnswer, entity.getAiExplanation(), entity.getNextReviewAt());
            return wrongNoteJpaRepository.save(entity).getId();
        } else {
            WrongNoteEntity newEntity = mapper.toEntity(userId, problemId, wrongAnswer, null);
            return wrongNoteJpaRepository.save(newEntity).getId();
        }
    }

    @Override
    @Transactional
    public Long updateWrongNote(WrongNote wrongNote) {

        if (wrongNote == null || wrongNote.getId() == null) {
            throw new EduQuestException(SubmissionDatabaseErrorCode.WRONG_NOTE_NOT_FOUND);
        }

        WrongNoteEntity wrongNoteEntity = wrongNoteJpaRepository.findById(wrongNote.getId())
                .orElseThrow(() -> new EduQuestException(SubmissionDatabaseErrorCode.WRONG_NOTE_NOT_FOUND));

        if (!wrongNoteEntity.getUserId().equals(wrongNote.getUserId())
                || !wrongNoteEntity.getProblemId().equals(wrongNote.getProblemId())) {
            throw new EduQuestException(SubmissionDatabaseErrorCode.WRONG_NOTE_NOT_FOUND);
        }

        wrongNoteEntity.updateWrongAnswer(
                wrongNote.getWrongAnswer(),
                wrongNote.getAiExplanation(),
                wrongNote.getNextReviewAt()
        );

        return wrongNoteJpaRepository.save(wrongNoteEntity).getId();

    }

    @Override
    @Transactional
    public void deleteByUserIdAndProblemId(Long userId, Long problemId) {
        wrongNoteJpaRepository.deleteByUserIdAndProblemId(userId, problemId);
    }

    @Override
    @Transactional
    public void markReviewed(Long userId, Long problemId, boolean isReviewed) {
        Optional<WrongNoteEntity> existing = findPrimaryWrongNote(userId, problemId);
        if (existing.isPresent()) {
            WrongNoteEntity entity = existing.get();
            if (isReviewed) {
                entity.markReviewed();
            } else {
                entity.updateWrongAnswer(entity.getWrongAnswer(), entity.getAiExplanation(), entity.getNextReviewAt());
            }
            wrongNoteJpaRepository.save(entity);
        }
    }

    @Override
    @Transactional
    public void deleteByUuid(UUID wrongNoteUuid) {
        wrongNoteJpaRepository.deleteByUuid(wrongNoteUuid);
    }

    private Optional<WrongNoteEntity> findPrimaryWrongNote(Long userId, Long problemId) {
        List<WrongNoteEntity> existing = wrongNoteJpaRepository.findAllByUserIdAndProblemIdOrderByUpdatedAtDescIdDesc(userId, problemId);

        if (existing.isEmpty()) {
            return Optional.empty();
        }

        WrongNoteEntity primary = existing.get(0);

        if (existing.size() > 1) {
            List<Long> duplicateIds = existing.stream()
                    .skip(1)
                    .map(WrongNoteEntity::getId)
                    .toList();
            wrongNoteJpaRepository.deleteAllByIdInBatch(duplicateIds);
        }

        return Optional.of(primary);
    }
}
