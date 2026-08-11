package com.veganing.domain.meal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * meals.ingredients jsonb 컬럼에 저장되는 식재료 스냅샷.
 * Entity가 아님 - ingredients 테이블과 FK 없음.
 * 분석 시점의 수치를 고정하기 위한 설계 (이후 ingredients 데이터가 바뀌어도 과거 기록 유지).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealIngredient implements Serializable {

    // 분석 당시 참조한 ingredient PK (FK가 아닌 참조용 ID)
    @JsonProperty("ingredient_id")
    private Long ingredientId;

    private String name;

    // 실제 섭취량 (g)
    @JsonProperty("amount_g")
    private Integer amountG;

    // 아래부터는 분석 시점 수치 스냅샷
    private BigDecimal co2;
    private BigDecimal calories;
    private BigDecimal protein;
    private BigDecimal fat;
    private BigDecimal carbs;
    private BigDecimal fiber;
    private BigDecimal calcium;
    private BigDecimal iron;

    // 비건 핵심 영양소 (결핍되기 쉬운 3가지)
    @JsonProperty("vitamin_b12")
    private BigDecimal vitaminB12;

    @JsonProperty("vitamin_d")
    private BigDecimal vitaminD;

    private BigDecimal omega3;
}