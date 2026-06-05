package com.eduquest.backend.infrastructure.persistence.learning.service;

import com.eduquest.backend.common.exception.EduQuestException;
import com.eduquest.backend.domain.learning.model.Hint;
import com.eduquest.backend.domain.learning.model.Problem;
import com.eduquest.backend.domain.learning.service.ProblemCommandService;
import com.eduquest.backend.infrastructure.persistence.learning.entity.HintEntity;
import com.eduquest.backend.infrastructure.persistence.learning.entity.ProblemEntity;
import com.eduquest.backend.infrastructure.persistence.learning.exception.LearningDatabaseErrorCode;
import com.eduquest.backend.infrastructure.persistence.learning.mapper.ProblemEntityMapper;
import com.eduquest.backend.infrastructure.persistence.learning.repository.HintJpaRepository;
import com.eduquest.backend.infrastructure.persistence.learning.repository.ProblemJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JpaProblemCommandService implements ProblemCommandService {

    private final ProblemJpaRepository problemJpaRepository;
    private final HintJpaRepository hintJpaRepository;
    private final ProblemEntityMapper problemEntityMapper;

    @Transactional
    @Override
    public Long saveProblem(Problem problem) {

        ProblemEntity entity = problemEntityMapper.toEntity(problem);
        entity = problemJpaRepository.save(entity);

        List<HintEntity> hintEntityList = new ArrayList<>();

        if (problem.getHints() != null && !problem.getHints().isEmpty()) {
            for (Hint hint : problem.getHints()) {
                HintEntity hintEntity = HintEntity.of(entity.getId(), hint.getLevel(), hint.getPoint(), hint.getContent());
                hintEntityList.add(hintEntity);
            }
        }

        hintJpaRepository.saveAll(hintEntityList);

        return entity.getId();
    }

    @Transactional
    @Override
    public Long updateProblem(Problem problem) {

        if (problem.getId() == null) {
            throw new EduQuestException(LearningDatabaseErrorCode.PROBLEM_NOT_FOUND);
        }

        Long problemId = problem.getId();

        ProblemEntity entity = problemJpaRepository.findById(problemId)
                .orElseThrow(() -> new EduQuestException(LearningDatabaseErrorCode.PROBLEM_NOT_FOUND));

        entity.update(
                problem.getStageId(),
                problem.getType(),
                problem.getNumber(),
                problem.getSummary(),
                problem.getExample(),
                problem.getExpectedOutput(),
                problem.getBlock()
        );

        List<HintEntity> oldHints = hintJpaRepository.findAllByProblemId(problemId);

        if (oldHints != null && !oldHints.isEmpty()) {
            hintJpaRepository.deleteAll(oldHints);
        }

        if (problem.getHints() != null && !problem.getHints().isEmpty()) {
            for (Hint hint : problem.getHints()) {
                HintEntity hintEntity = HintEntity.of(problemId, hint.getLevel(), hint.getPoint(), hint.getContent());
                hintJpaRepository.save(hintEntity);
            }
        }

        return problemJpaRepository.save(entity).getId();

    }

    @Transactional
    @Override
    public void deleteProblem(UUID uuid) {

        problemJpaRepository.findByUuid(uuid).ifPresent(e -> {
            List<HintEntity> old = hintJpaRepository.findAllByProblemId(e.getId());

            if (old != null && !old.isEmpty()) {
                hintJpaRepository.deleteAll(old);
            }

            problemJpaRepository.delete(e);
        });

    }

}
