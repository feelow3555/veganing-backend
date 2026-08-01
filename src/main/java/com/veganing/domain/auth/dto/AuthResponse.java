package com.veganing.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// AuthResponse 는 로그인 성공했을 때 클라이언트한테 토큰 돌려주는 그릇

@Getter
@AllArgsConstructor
public class AuthResponse {
    private String accessToken; // 이제 앞으로 이걸로 인증함
    private String tokenType; // Bearer -> HTTP

    // of() -> 정적 팩토리 메서드 new AuthResponse() 대신 씀 호출하는 쪽에서 tokenType 을 신경 안 써도 되게 숨겨놓은 거고, 항상 "Bearer"로 고정되게 강제
    public static AuthResponse of(String accessToken) {
        return new AuthResponse(accessToken, "Bearer");
    }
}
