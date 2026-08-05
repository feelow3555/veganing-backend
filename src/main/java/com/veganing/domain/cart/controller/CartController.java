package com.veganing.domain.cart.controller;

import com.veganing.domain.cart.dto.CartAddRequest;
import com.veganing.domain.cart.dto.CartResponse;
import com.veganing.domain.cart.dto.CartUpdateRequest;
import com.veganing.domain.cart.service.CartService;
import com.veganing.global.auth.CustomUserDetails;
import com.veganing.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    // 장바구니 조회
    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        // 1. cartService.getCart(userId) 호출
        CartResponse response = cartService.getCart(userDetails.getUserId());

        // 2. 200 OK + ApiResponse.success() 반환
        return ResponseEntity.ok(ApiResponse.success("장바구니 조회 성공", response));
    }

    // 상품 담기
    @PostMapping
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @Valid @RequestBody CartAddRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        // 1. cartService.addItem(userId, request) 호출
        CartResponse response = cartService.addItem(userDetails.getUserId(), request);

        // 2. 200 OK + ApiResponse.success() 반환
        return ResponseEntity.ok(ApiResponse.success("상품 담기 성공", response));
    }

    // 수량 변경
    @PutMapping("/{cartItemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateItem(
            @PathVariable Long cartItemId,
            @Valid @RequestBody CartUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        // 1. cartService.updateItem(userId, cartItemId, request) 호출
        CartResponse response = cartService.updateItem(userDetails.getUserId(), cartItemId, request);

        // 2. 200 OK + ApiResponse.success() 반환
        return ResponseEntity.ok(ApiResponse.success("수량 변경 성공", response));
    }

    // 상품 제거
    @DeleteMapping("/{cartItemId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @PathVariable Long cartItemId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        // 1. cartService.removeItem(userId, cartItemId) 호출
        CartResponse response = cartService.removeItem(userDetails.getUserId(), cartItemId);

        // 2. 200 OK + ApiResponse.success() 반환
        return ResponseEntity.ok(ApiResponse.success("상품 제거 성공", response));
    }
}
