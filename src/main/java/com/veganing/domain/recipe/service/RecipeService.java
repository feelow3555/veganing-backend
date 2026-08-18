package com.veganing.domain.recipe.service;

import com.veganing.domain.recipe.dto.RecipeResponse;
import com.veganing.domain.recipe.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecipeService {

    private final RecipeRepository recipeRepository;

    public List<RecipeResponse> getTodayRecipes() {
        return recipeRepository.findAll().stream()
                .map(RecipeResponse::from)
                .collect(Collectors.toList());
    }
}
