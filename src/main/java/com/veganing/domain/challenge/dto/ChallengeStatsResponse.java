package com.veganing.domain.challenge.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChallengeStatsResponse {

    private Integer totalChallenges;
    private Integer completedChallenges;
    private Integer currentStreak;
    private Integer totalPoints;
}
