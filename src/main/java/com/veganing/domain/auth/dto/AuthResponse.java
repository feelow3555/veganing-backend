package com.veganing.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {
    private String accessToken;
    private String tokenType;
    private Long userId;
    private String email;
    private String nickname;

    public static AuthResponse of(String accessToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .build();
    }

    public static AuthResponse ofProfile(String email, String nickname) {
        return AuthResponse.builder()
                .email(email)
                .nickname(nickname)
                .build();
    }
}