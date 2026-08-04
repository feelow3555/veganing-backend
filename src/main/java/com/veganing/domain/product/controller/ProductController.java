package com.veganing.domain.product.controller;

import com.veganing.domain.product.dto.ProductResponse;
import com.veganing.domain.product.service.ProductService;
import com.veganing.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/product")
public class ProductController {

    private final ProductService productService;

    // 상품 목록 조회 (페이징)
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getProducts(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) { // 기본값: 10개씩, id 기준 내림차순 (최신 상품 먼저)
        return ResponseEntity.ok(ApiResponse.success("상품 목록 조회 성공", productService.getProducts(pageable)));
    }

    // 상품 단건 조회
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success("상품 상세 조회 성공", productService.getProduct(productId)));
    }
}
