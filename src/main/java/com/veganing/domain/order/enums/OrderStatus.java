package com.veganing.domain.order.enums;

public enum OrderStatus {
    PENDING,    // 주문 대기 (결제 전)
    PAID,       // 결제 완료
    SHIPPED,    // 배송 중
    DELIVERED,  // 배송 완료
    CANCELLED   // 주문 취소
}
