package com.veganing.domain.recipe.controller;

import com.veganing.domain.recipe.dto.RecipeResponse;
import com.veganing.domain.recipe.service.RecipeService;
import com.veganing.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recipe")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;

    @GetMapping("/today")
    public ResponseEntity<ApiResponse<List<RecipeResponse>>> getTodayRecipes() {
        List<RecipeResponse> recipes = recipeService.getTodayRecipes();
        return ResponseEntity.ok(ApiResponse.success("오늘의 레시피 조회 성공", recipes));
    }
}
