package com.veganing.global.error;

import lombok.Getter;

/*
    RuntimeException       → 에러 메시지만 있음
    CustomException        → 에러 메시지 + HttpStatus 코드까지 있음
*/


@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
