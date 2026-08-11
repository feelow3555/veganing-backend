package com.veganing.global.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // AUTH
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용중인 이메일입니다"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 유저입니다"),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 틀렸습니다"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다"),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다"),
    // Community
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 게시물입니다"),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 댓글입니다"),
    UNAUTHORIZED(HttpStatus.FORBIDDEN, "권한이 없습니다"),
    // Challenge
    CHALLENGE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 진행중인 챌린지가 있습니다"),
    CHALLENGE_NOT_FOUND(HttpStatus.NOT_FOUND, "진행중인 챌린지가 없습니다"),
    // Product
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 상품입니다"),
    // Cart
    CART_NOT_FOUND(HttpStatus.NOT_FOUND, "장바구니를 찾을 수 없습니다"),
    CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "장바구니 상품을 찾을 수 없습니다"),
    // Order
    CART_EMPTY(HttpStatus.BAD_REQUEST, "장바구니가 비어있습니다"),
    OUT_OF_STOCK(HttpStatus.CONFLICT, "재고가 부족합니다"),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 주문입니다"),

    // Meal
    MEAL_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 식단입니다"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
