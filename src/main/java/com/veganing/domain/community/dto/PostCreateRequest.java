package com.veganing.domain.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
public class PostCreateRequest {

    @NotBlank(message = "제목을 입력해주세요")
    private String title;

    private String content; // 선택

    @NotBlank(message = "이미지를 업로드해주세요")
    private String imageUrl;

    @NotEmpty(message = "재료를 입력해주세요")
    private List<Map<String, String>> ingredients;

    @NotEmpty(message = "레시피 순서를 입력해주세요")
    private List<Map<String, String>> steps;
}
