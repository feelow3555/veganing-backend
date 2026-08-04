package com.veganing.domain.challenge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PointAddRequest {

    @NotNull
    private Integer points;

    @NotBlank
    private String reason;
}
