package com.eduquest.backend.application.submission.dto;

import java.time.LocalDateTime;

public record EvaluationInfo(Boolean isCorrect, LocalDateTime createdAt, boolean pending) {

    public static EvaluationInfo of(Boolean isCorrect, LocalDateTime createdAt) {
        return new EvaluationInfo(isCorrect, createdAt, false);
    }

    public static EvaluationInfo pendingInfo() {
        return new EvaluationInfo(null, null, true);
    }

}
