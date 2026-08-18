package com.veganing.domain.community.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LikeResponse {
    private Integer likeCount;
    private boolean liked;
}
