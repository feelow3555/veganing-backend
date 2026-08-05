package com.veganing.domain.cart.service;

import com.veganing.domain.cart.dto.CartAddRequest;
import com.veganing.domain.cart.dto.CartItemResponse;
import com.veganing.domain.cart.dto.CartResponse;
import com.veganing.domain.cart.dto.CartUpdateRequest;
import com.veganing.domain.cart.entity.Cart;
import com.veganing.domain.cart.entity.CartItem;
import com.veganing.domain.cart.repository.CartItemRepository;
import com.veganing.domain.cart.repository.CartRepository;
import com.veganing.domain.product.entity.Product;
import com.veganing.domain.product.repository.ProductRepository;
import com.veganing.global.error.CustomException;
import com.veganing.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    // 장바구니 조회 메서드
    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        // 1. userId로 cart 조회 (없으면 CART_NOT_FOUND 예외)
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.CART_NOT_FOUND));

        // 2. cart.getItems() 스트림으로 CartItemResponse 리스트 변환
        //    - totalPrice = price × quantity
        List<CartItemResponse> items = cart.getItems().stream()
                .map(item -> CartItemResponse.builder()
                        .cartItemId(item.getId())
                        .productId(item.getProduct().getId())
                        .price(item.getProduct().getPrice())
                        .productName(item.getProduct().getName())
                        .imageUrl(item.getProduct().getImageUrl())
                        .quantity(item.getQuantity())
                        .totalPrice(item.getProduct().getPrice()
                                .multiply(BigDecimal.valueOf(item.getQuantity())))
                        .build())
                .toList();

        // 3. totalAmount = items 전체 totalPrice 합산
        BigDecimal totalAmount = items.stream()
                .map(CartItemResponse::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 4. CartResponse 빌드 후 반환
        return CartResponse.builder()
                .cartId(cart.getId())
                .items(items)
                .totalAmount(totalAmount)
                .build();
    }

    // 상품 담기 메서드
    @Transactional
    public CartResponse addItem(Long userId, CartAddRequest request) {
        // 1. userId로 cart 조회 (없으면 CART_NOT_FOUND 예외)
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.CART_NOT_FOUND));

        // 2. productId로 product 조회 (없으면 PRODUCT_NOT_FOUND 예외)
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        // 3. 이미 담긴 상품인지 확인 (findByCartIdAndProductId)
        //    - 있으면 → addQuantity()로 수량 추가
        //    - 없으면 → 새 CartItem 생성 후 저장
        cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())
                .ifPresentOrElse(
                        item -> item.addQuantity(request.getQuantity()),
                        () -> cartItemRepository.save(CartItem.builder()
                                .cart(cart)
                                .product(product)
                                .quantity(request.getQuantity())
                                .build())
                );

        // 4. getCart(userId) 호출해서 최신 장바구니 반환
        return getCart(userId);
    }

    // 수량 변경 메서드
    @Transactional
    public CartResponse updateItem(Long userId, Long cartItemId, CartUpdateRequest request) {
        // 1. cartItemId로 CartItem 조회 (없으면 CART_ITEM_NOT_FOUND 예외)
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new CustomException(ErrorCode.CART_ITEM_NOT_FOUND));

        // 2. 본인 항목인지 확인 (cartItem.getCart().getUser().getId() vs userId)
        //    - 다르면 → UNAUTHORIZED 예외
        if(!cartItem.getCart().getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        // 3. updateQuantity()로 수량 변경
        cartItem.updateQuantity(request.getQuantity());

        // 4. getCart(userId) 호출해서 최신 장바구니 반환
        return getCart(userId);
    }

    // 상품 제거
    @Transactional
    public CartResponse removeItem(Long userId, Long cartItemId) {
        // 1. cartItemId로 CartItem 조회 (없으면 CART_ITEM_NOT_FOUND 예외)
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new CustomException(ErrorCode.CART_ITEM_NOT_FOUND));

        // 2. 본인 항목인지 확인 (cartItem.getCart().getUser().getId() vs userId)
        //    - 다르면 → UNAUTHORIZED 예외
        if(!cartItem.getCart().getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        // 3. cartItemRepository.delete()로 삭제
        cartItemRepository.delete(cartItem);

        // 4. getCart(userId) 호출해서 최신 장바구니 반환
        return getCart(userId);
    }
}
