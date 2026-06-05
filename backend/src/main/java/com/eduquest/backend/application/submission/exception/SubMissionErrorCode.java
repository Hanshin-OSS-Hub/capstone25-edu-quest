package com.eduquest.backend.application.submission.exception;

import com.eduquest.backend.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SubMissionErrorCode implements ErrorCode {

    INVALID_SUBMISSION_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 제출 요청입니다."),
    UNAUTHENTICATED_SUBMISSION(HttpStatus.UNAUTHORIZED, "인증 정보가 없습니다."),
    FORBIDDEN_SUBMISSION_ACCESS(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    EVALUATION_PENDING(HttpStatus.ACCEPTED, "평가가 진행 중입니다."),
    EVALUATION_FAILED(HttpStatus.NO_CONTENT, "평가가 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String message;

}
