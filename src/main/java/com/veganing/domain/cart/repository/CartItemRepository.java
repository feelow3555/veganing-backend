package com.veganing.domain.cart.repository;

import com.veganing.domain.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // 상품 담기 시 이미 담긴 상품인지 확인 (중복 체크용)
    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);
}
