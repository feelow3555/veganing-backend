package com.veganing.domain.order.service;

import com.veganing.domain.cart.entity.Cart;
import com.veganing.domain.cart.entity.CartItem;
import com.veganing.domain.cart.repository.CartItemRepository;
import com.veganing.domain.cart.repository.CartRepository;
import com.veganing.domain.order.dto.OrderCreateRequest;
import com.veganing.domain.order.dto.OrderItemResponse;
import com.veganing.domain.order.dto.OrderResponse;
import com.veganing.domain.order.entity.Order;
import com.veganing.domain.order.entity.OrderItem;
import com.veganing.domain.order.enums.OrderStatus;
import com.veganing.domain.order.repository.OrderItemRepository;
import com.veganing.domain.order.repository.OrderRepository;
import com.veganing.domain.product.entity.Product;
import com.veganing.domain.product.repository.ProductRepository;
import com.veganing.domain.auth.entity.User;
import com.veganing.domain.auth.repository.UserRepository;
import com.veganing.global.error.CustomException;
import com.veganing.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    // 주문 생성
    // 비관적 락으로 재고 차감 → 동시 주문 시 재고 초과 방지
    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request, String email) {

        // 1. 유저 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 장바구니 조회
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.CART_NOT_FOUND));

        // 3. 장바구니 상품 목록 조회
        List<CartItem> cartItems = cart.getItems();

        // 4. 장바구니가 비어있으면 예외
        if (cartItems.isEmpty()) {
            throw new CustomException(ErrorCode.CART_EMPTY);
        }

        // 5. Order 먼저 생성 (배송지 정보 스냅샷 저장)
        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.ZERO) // 일단 0으로 초기화, 아래서 합산 후 업데이트
                .recipientName(request.getRecipientName())
                .recipientPhone(request.getRecipientPhone())
                .address(request.getAddress())
                .addressDetail(request.getAddressDetail())
                .memo(request.getMemo())
                .build();

        orderRepository.save(order);

        // 6. cart_items 순회 → 각 상품 비관적 락으로 조회 + 재고 차감 + OrderItem 생성
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem cartItem : cartItems) {

            // 비관적 락으로 상품 조회 (SELECT FOR UPDATE)
            // → 동시에 여러 유저가 같은 상품 주문해도 재고 정합성 보장
            Product product = productRepository.findByIdWithLock(cartItem.getProduct().getId())
                    .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

            // 재고 확인
            if (product.getStock() < cartItem.getQuantity()) {
                throw new CustomException(ErrorCode.OUT_OF_STOCK);
            }

            // 재고 차감
            product.decreaseStock(cartItem.getQuantity());

            // 주문 시점 가격 스냅샷으로 OrderItem 생성
            // → 이후 상품 가격이 바뀌어도 주문 내역은 당시 가격 유지
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(cartItem.getQuantity())
                    .priceAtOrder(product.getPrice()) // 현재 가격을 스냅샷으로 저장
                    .build();

            order.addOrderItem(orderItem);
            // 총 금액 누적
            BigDecimal subtotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalAmount = totalAmount.add(subtotal);
        }

        // 7. 총 금액 업데이트
        order.updateTotalAmount(totalAmount);

        // 8. 주문 완료 후 장바구니 비우기
        cartItemRepository.deleteAll(cartItems);

        // 9. 응답 반환
        return toOrderResponse(order);
    }

    // 내 주문 목록 조회
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(String email) {

        // 유저 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 주문 목록 조회 (최신순)
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        // Order → OrderResponse 변환
        return orders.stream()
                .map(this::toOrderResponse)
                .toList();
    }

    // 주문 상세 조회
    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId, String email) {

        // 유저 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 주문 조회
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // 본인 주문인지 확인
        if (!order.getUser().getId().equals(user.getId())) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        return toOrderResponse(order);
    }

    // Order → OrderResponse 변환 (공통 메서드)
    private OrderResponse toOrderResponse(Order order) {

        // OrderItem → OrderItemResponse 변환
        List<OrderItemResponse> itemResponses = order.getOrderItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .quantity(item.getQuantity())
                        .priceAtOrder(item.getPriceAtOrder())
                        .subtotal(item.getPriceAtOrder()
                                .multiply(BigDecimal.valueOf(item.getQuantity())))
                        .build())
                .toList();

        return OrderResponse.builder()
                .orderId(order.getId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .address(order.getAddress())
                .addressDetail(order.getAddressDetail())
                .memo(order.getMemo())
                .createdAt(order.getCreatedAt())
                .items(itemResponses)
                .build();
    }
}