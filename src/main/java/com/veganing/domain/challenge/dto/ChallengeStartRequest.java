package com.veganing.domain.challenge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChallengeStartRequest {

    // 챌린지 종류 (ex. "30일 비건 챌린지")
    @NotBlank
    private String type;

    // 챌린지 기간 (일)
    @NotNull
    private Integer duration;

    // 챌린지 목적
    @NotBlank
    private String purpose;
}
