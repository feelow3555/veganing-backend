package com.veganing.domain.community.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class PostResponse {

    private Long id;
    private String title;
    private String content;
    private String imageUrl;
    private List<Map<String, String>> ingredients;
    private List<Map<String, String>> steps;
    private String nickname;
    private Integer likeCount;
    private int commentCount;
    private LocalDateTime createdAt;
}
