package com.veganing.domain.order.entity;

import com.veganing.domain.auth.entity.User;
import com.veganing.domain.order.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Order.java 전체

@Entity
@Table(name = "orders")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount; // totalPrice → totalAmount 로 변경

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // 배송지 컬럼
    private String recipientName;
    private String recipientPhone;
    private String address;
    private String addressDetail;
    private String memo;

    // OrderItem 연관관계 추가
    // cascade = PERSIST → Order 저장 시 OrderItem도 함께 저장
    // orphanRemoval = true → Order에서 제거된 OrderItem은 DB에서도 삭제
    @OneToMany(mappedBy = "order", cascade = CascadeType.PERSIST, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    // 총 금액 업데이트 메서드
    // Order 생성 시 0으로 초기화 후 OrderItem 전부 생성 완료되면 호출
    public void updateTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void addOrderItem(OrderItem orderItem) {
        // orderItems 리스트에 직접 추가
        // → toOrderResponse() 호출 시 리스트에서 바로 꺼낼 수 있음
        this.orderItems.add(orderItem);
    }
}
