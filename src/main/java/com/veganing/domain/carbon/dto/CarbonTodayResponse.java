package com.veganing.domain.carbon.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CarbonTodayResponse {
    private BigDecimal totalCarbon;  // 오늘 식재료 탄소 발자국 합산 (kg)
    private int mealCount;           // 오늘 식단 수
    private BigDecimal savedCarbon;  // 한국인 평균(5.5kg) 대비 절감량 (음수면 평균보다 많이 배출)
}
