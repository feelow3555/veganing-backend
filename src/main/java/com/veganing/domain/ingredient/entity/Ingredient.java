package com.veganing.domain.ingredient.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ingredients")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 식재료명
    @Column(nullable = false)
    private String name;

    // 식재료명 영어 매핑 (AI용)
    @Column(name = "name_en")
    private String nameEn;

    private String category;

    // 100g당 CO2 절감량 (kg)
    @Column(name = "co2_per_100g", precision = 10, scale = 4)
    private BigDecimal co2Per100g;

    // 기본 영양소
    @Column(precision = 10, scale = 2)
    private BigDecimal calories;

    @Column(precision = 10, scale = 2)
    private BigDecimal protein;

    @Column(precision = 10, scale = 2)
    private BigDecimal fat;

    @Column(precision = 10, scale = 2)
    private BigDecimal carbs;

    @Column(precision = 10, scale = 2)
    private BigDecimal fiber;

    @Column(precision = 10, scale = 2)
    private BigDecimal calcium;

    @Column(precision = 10, scale = 2)
    private BigDecimal iron;

    // 비건 핵심 영양소 (결핍되기 쉬운 3가지)
    @Column(name = "vitamin_b12", precision = 10, scale = 4)
    private BigDecimal vitaminB12;

    @Column(name = "vitamin_d", precision = 10, scale = 4)
    private BigDecimal vitaminD;

    @Column(precision = 10, scale = 4)
    private BigDecimal omega3;

    // 데이터 출처 (식품의약품안전처, Our World in Data 등)
    private String source;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
