package com.veganing.domain.meal.controller;

import com.veganing.domain.meal.dto.MealAnalyzeRequest;
import com.veganing.domain.meal.dto.MealResponse;
import com.veganing.domain.meal.dto.RecommendResponse;
import com.veganing.domain.meal.service.MealService;
import com.veganing.global.auth.CustomUserDetails;
import com.veganing.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/meal")
@RequiredArgsConstructor
public class MealController {

    private final MealService mealService;

    // 1. S3 Presigned URL 발급
    @GetMapping("/upload-url")
    public ResponseEntity<ApiResponse<Map<String, String>>> getUploadUrl(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        String presignedUrl = mealService.getUploadUrl(userDetails.getEmail());
        String imageUrl = presignedUrl.split("\\?")[0]; // 쿼리스트링 제거
        return ResponseEntity.ok(ApiResponse.success("Presigned URL 발급 성공",
                Map.of("uploadUrl", presignedUrl, "imageUrl", imageUrl)));
    }

    // 2. 식단 분석 요청 → 즉시 202 반환
    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Long>>> analyzeMeal(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody MealAnalyzeRequest request
    ) {
        Long mealId = mealService.analyzeMeal(request, userDetails.getEmail());
        return ResponseEntity.accepted()
                .body(ApiResponse.success("분석 요청 완료. 잠시 후 결과를 확인하세요.",
                        Map.of("mealId", mealId)));
    }

    // 3. 식단 기록 목록 조회
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<MealResponse>>> getMealHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<MealResponse> result = mealService.getHistory(userDetails.getEmail());
        return ResponseEntity.ok(ApiResponse.success("식단 기록 조회 성공", result));
    }

    // 4. 단건 조회 (폴링용 - ANALYZING → DONE/FAILED 확인)
    @GetMapping("/{mealId}")
    public ResponseEntity<ApiResponse<MealResponse>> getMeal(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long mealId
    ) {
        MealResponse response = mealService.getMeal(mealId, userDetails.getEmail());
        return ResponseEntity.ok(ApiResponse.success("식단 조회 성공", response));
    }

    // 5. RAG 기반 식단 추천
// 최근 완료 식단 분석 결과 → Voyage AI 임베딩 → pgvector 유사 레시피 검색 → Claude 추천 생성
    @GetMapping("/recommend")
    public ResponseEntity<ApiResponse<RecommendResponse>> recommend(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        RecommendResponse response = mealService.recommend(userDetails.getEmail());
        return ResponseEntity.ok(ApiResponse.success("식단 추천 성공", response));
    }
}