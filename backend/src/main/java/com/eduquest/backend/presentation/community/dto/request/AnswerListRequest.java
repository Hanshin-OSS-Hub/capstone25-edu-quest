package com.eduquest.backend.presentation.community.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.RequestParam;

public record AnswerListRequest(
        @NotNull(message = "페이지 번호는 필수입니다.")
        Integer page,
        @NotNull(message = "페이지 크기는 필수입니다.")
        Integer size,
        @JsonProperty("is_asc")
        @RequestParam(name = "is_asc")
        Boolean isAsc
) {

    public static AnswerListRequest of(int page, int size, Boolean isAsc) {
        return new AnswerListRequest(page, size, isAsc);
    }

}
