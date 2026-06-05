package com.eduquest.backend.application.progress.dto;

import java.util.List;

/**
 * Application 계층에서 사용하는 Progress DTO 모음
 */
public final class ProgressDto {

    private ProgressDto() {
    }

    public record ProgressItem(String stage, Integer stageNumber, int totalQuestionCount, List<Integer> clear) {
    }

}
