package com.veganing.domain.challenge.dto;

import com.veganing.domain.challenge.entity.ChallengeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class ChallengeResponse {

    private Long id;
    private ChallengeType type;
    private Integer durationDays;
    private String status;
    private Integer currentDay;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
}
