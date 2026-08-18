// com/veganing/domain/community/dto/RankingResponse.java
package com.veganing.domain.community.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RankingResponse {
    private Long userId;
    private String nickname;
    private String region;
    private Integer totalPoints;
    private Integer rank;
}