package com.veganing.global.error;

import com.veganing.global.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/*
    Service 에서 CustomException 던짐
                ↓
    GlobalExceptionHandler 가 잡아서
                ↓
    ApiResponse.error()로 포맷 맞춰서 반환
*/

@RestControllerAdvice
// @RestControllerAdvice - 모든 Controller 에서 발생하는 예외를 여기서 잡아줘. Controller 마다 try-catch 안 써도 되는 이유야.
public class GlobalExceptionHandler {

    // CustomException 처리
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .body(ApiResponse.error(e.getErrorCode().getMessage()));
    }

    // 예상치 못한 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        return ResponseEntity
                .status(500)
                .body(ApiResponse.error("서버 오류가 발생했습니다."));
    }
}
