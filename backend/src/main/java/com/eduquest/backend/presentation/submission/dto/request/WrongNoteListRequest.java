package com.eduquest.backend.presentation.submission.dto.request;

import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.RequestParam;

public record WrongNoteListRequest(
        @Min(value = 0, message = "page는 0 이상이어야 합니다.") int page,
        @Min(value = 1, message = "size는 1 이상이어야 합니다.") int size,
        String sort,
        @RequestParam(name = "is_asc")
        Boolean isAsc
) {

    public static WrongNoteListRequest of(int page, int size, String sort, Boolean isAsc) {
        return new WrongNoteListRequest(page, size, sort, isAsc);
    }

}
