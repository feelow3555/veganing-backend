package com.veganing.domain.product.repository;

import com.veganing.domain.product.entity.Product;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

// 상품 목록 페이징 조회

public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findAll(Pageable pageable);

    /**
     * 비관적 락을 걸고 상품 단건 조회
     * ORDER API 에서 재고 차감 시 사용 (SELECT FOR UPDATE)
     * 동시에 여러 주문이 들어와도 한 트랜잭션씩 순서대로 처리됨
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithLock(@Param("id") Long id);

    // 좋아요는 카운트가 틀려도 재시도하면 그만이지만, 재고는 마이너스가 되면 치명적이라 비관적 락을 선택
}
