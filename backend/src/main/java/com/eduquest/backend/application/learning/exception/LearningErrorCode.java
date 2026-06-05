package com.eduquest.backend.application.learning.exception;

import com.eduquest.backend.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum LearningErrorCode implements ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "학습 도메인 유효성 검증 실패"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "학습 데이터가 존재하지 않습니다."),
    DUPLICATE(HttpStatus.CONFLICT, "이미 존재하는 학습 데이터입니다."),
    FORBIDDEN_PROBLEM_ACCESS(HttpStatus.FORBIDDEN, "문제에 접근할 권한이 없습니다."),
    INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "코인이 부족합니다.");

    private final HttpStatus httpStatus;
    private final String message;

}
