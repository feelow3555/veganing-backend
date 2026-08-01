package com.veganing.global.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // AUTH (ErrorCode enum 으로 관리 나중에 에러 추가시 수정)
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용중인 이메일입니다"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 유저입니다"),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 틀렸습니다"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다"),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
