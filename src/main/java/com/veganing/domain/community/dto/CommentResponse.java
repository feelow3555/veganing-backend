package com.veganing.domain.community.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CommentResponse {

    private Long id;
    private String nickname;
    private String content;
    private LocalDateTime createdAt;
}
