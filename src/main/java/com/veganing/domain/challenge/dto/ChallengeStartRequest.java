package com.veganing.domain.challenge.dto;

import com.veganing.domain.challenge.entity.ChallengeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChallengeStartRequest {

    // String → ChallengeType (VEGAN, FLEXITARIAN 등 Enum값으로 받음)
    @NotNull
    private ChallengeType type;

    @NotNull
    private Integer duration;

    @NotBlank
    private String purpose;
}