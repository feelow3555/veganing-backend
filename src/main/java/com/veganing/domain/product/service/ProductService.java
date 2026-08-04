package com.veganing.domain.product.service;

import com.veganing.domain.product.dto.ProductResponse;
import com.veganing.domain.product.entity.Product;
import com.veganing.domain.product.repository.ProductRepository;
import com.veganing.global.error.CustomException;
import com.veganing.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 조회 전용 Service → 모든 메서드 읽기 전용 트랜잭션 적용
public class ProductService {

    private final ProductRepository productRepository;

    // 상품 목록 페이징 조회 메서드
    public Page<ProductResponse> getProducts(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(product -> ProductResponse.builder()
                        .id(product.getId())
                        .name(product.getName())
                        .description(product.getDescription())
                        .price(product.getPrice())
                        .stock(product.getStock())
                        .imageUrl(product.getImageUrl())
                        .category(product.getCategory())
                        .build());
    }

    // 상품 단건 조회
    public ProductResponse getProduct(Long productId) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .imageUrl(product.getImageUrl())
                .category(product.getCategory())
                .build();
    }
}
