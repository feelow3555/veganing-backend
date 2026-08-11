package com.veganing.domain.meal.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealAnalyzeRequest {

    // 프론트가 S3에 직접 업로드한 이미지 URL
    @NotBlank(message = "이미지 URL을 입력해주세요")
    private String imageUrl;

    // 음식 이름 (벤치마크 조건 B - 텍스트 힌트)
    @NotBlank(message = "음식 이름을 입력해주세요")
    private String foodName;

    // 음식 설명 (선택, 없으면 빈 문자열)
    private String foodDescription = "";
}