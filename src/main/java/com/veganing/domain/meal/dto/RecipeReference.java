package com.veganing.domain.meal.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RecipeReference {
    private String title;
    private List<String> ingredients;
    private String imageUrl;
}