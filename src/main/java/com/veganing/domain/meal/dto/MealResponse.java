package com.veganing.domain.meal.dto;

import com.veganing.domain.meal.entity.Meal;
import com.veganing.domain.meal.enums.MealStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class MealResponse {

    private Long id;
    private String imageUrl;
    private List<MealIngredient> ingredients;
    private BigDecimal totalCarbon;
    private Map<String, BigDecimal> nutrition;
    private String aiFeedback;
    private MealStatus status;
    private Boolean isVeganCompliant;
    private List<String> confirmedViolations;
    private List<Meal.SuspectedViolation> suspectedViolations;
    private LocalDateTime createdAt;

    public static MealResponse from(Meal meal) {
        return MealResponse.builder()
                .id(meal.getId())
                .imageUrl(meal.getImageUrl())
                .ingredients(meal.getIngredients())
                .totalCarbon(meal.getTotalCarbon())
                .nutrition(meal.getNutrition())
                .aiFeedback(meal.getAiFeedback())
                .status(meal.getStatus())
                .isVeganCompliant(meal.getIsVeganCompliant())
                .confirmedViolations(meal.getConfirmedViolations())
                .suspectedViolations(meal.getSuspectedViolations())
                .createdAt(meal.getCreatedAt())
                .build();
    }
}