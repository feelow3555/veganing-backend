package com.veganing.domain.recipe.dto;

import com.veganing.domain.recipe.entity.Recipe;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RecipeResponse {
    private Long id;
    private String title;
    private String content;
    private String imageUrl;
    private LocalDateTime createdAt;

    public static RecipeResponse from(Recipe recipe) {
        return RecipeResponse.builder()
                .id(recipe.getId())
                .title(recipe.getTitle())
                .content(recipe.getContent())
                .imageUrl(recipe.getImageUrl())
                .createdAt(recipe.getCreatedAt())
                .build();
    }
}
