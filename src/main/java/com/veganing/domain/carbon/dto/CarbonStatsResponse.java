package com.veganing.domain.carbon.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CarbonStatsResponse {
    private BigDecimal totalCarbon;   // 전체 누적 절감량
    private int totalMealCount;       // 전체 식단 수
    private BigDecimal avgDailyCarbon; // 일평균 절감량
}
