package com.veganing.domain.carbon.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CarbonTodayResponse {
    private BigDecimal totalCarbon;  // 오늘 절감량
    private int mealCount;           // 오늘 식단 수
}
