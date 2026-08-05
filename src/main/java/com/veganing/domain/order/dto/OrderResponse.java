package com.veganing.domain.order.dto;

import com.veganing.domain.order.enums.OrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class OrderResponse {

    private Long orderId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private String recipientName;
    private String recipientPhone;
    private String address;
    private String addressDetail;
    private String memo;
    private LocalDateTime createdAt;
    private List<OrderItemResponse> items;
}