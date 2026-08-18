// com/veganing/domain/auth/dto/MeResponse.java
package com.veganing.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class MeResponse {
    private Long userId;
    private String email;
    private String nickname;
    private String region;
    private LocalDateTime createdAt;
}