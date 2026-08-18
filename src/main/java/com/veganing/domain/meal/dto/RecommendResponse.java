package com.veganing.domain.meal.dto;

import com.veganing.domain.meal.dto.RecipeReference;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RecommendResponse {

    // Claude 가 생성한 개인화 추천 텍스트
    private String recommendation;

    // 추천 근거로 사용된 레시피 제목 목록
    // 프론트에서 "이런 레시피를 참고했어요" 표시용
    private List<RecipeReference> referenceRecipes;
}