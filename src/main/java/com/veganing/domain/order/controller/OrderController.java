package com.veganing.domain.order.controller;

import com.veganing.domain.order.dto.OrderCreateRequest;
import com.veganing.domain.order.dto.OrderResponse;
import com.veganing.domain.order.service.OrderService;
import com.veganing.global.auth.CustomUserDetails;
import com.veganing.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;

    // 주문 생성
    // 장바구니 전체를 주문으로 전환 + 재고 차감 (비관적 락)
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody OrderCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        OrderResponse response = orderService.createOrder(request, userDetails.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("주문이 완료되었습니다", response));
    }

    // 내 주문 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<OrderResponse> response = orderService.getMyOrders(userDetails.getEmail());
        return ResponseEntity.ok(ApiResponse.success("주문 목록 조회 성공", response));
    }

    // 주문 상세 조회
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        OrderResponse response = orderService.getOrder(orderId, userDetails.getEmail());
        return ResponseEntity.ok(ApiResponse.success("주문 상세 조회 성공", response));
    }
}