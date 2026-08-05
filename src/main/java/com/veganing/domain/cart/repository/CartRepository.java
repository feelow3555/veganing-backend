package com.veganing.domain.cart.repository;

import com.veganing.domain.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    // 장바구니 조회 시 유저 기준으로 cart 찾기
    Optional<Cart> findByUserId(Long userId);
}
