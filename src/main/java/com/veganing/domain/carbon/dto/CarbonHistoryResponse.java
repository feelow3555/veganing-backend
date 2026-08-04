package com.veganing.domain.carbon.dto;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class CarbonHistoryResponse {
    private LocalDate date;
    private BigDecimal totalCarbon;
    private int mealCount;
}
