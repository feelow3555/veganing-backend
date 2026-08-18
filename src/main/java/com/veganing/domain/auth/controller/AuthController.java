package com.veganing.domain.auth.controller;

// Controller 는 클라이언트 요청을 받아서 Service 한테 넘기고 결과를 반환하는 창구

import com.veganing.domain.auth.dto.AuthResponse;
import com.veganing.domain.auth.dto.LoginRequest;
import com.veganing.domain.auth.dto.MeResponse;
import com.veganing.domain.auth.dto.SignupRequest;
import com.veganing.domain.auth.service.AuthService;
import com.veganing.global.auth.CustomUserDetails;
import com.veganing.global.auth.JwtUtil;
import com.veganing.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth") // 이 Controller 의 모든 URL 앞에 /api/auth 가 붙음
public class AuthController {

    private final AuthService authService; // Service 주입. Controller 가 Service 를 써야 하니까
    private final JwtUtil jwtUtil;

    // 회원가입
    @PostMapping("/signup") // POST 요청이 /api/auth/signup 으로 오면 이 메서드가 실행
    // @Valid - SignupRequest 에 붙인 @NotBlank, @Email, @Size 검증을 여기서 실행
    // @RequestBody - 클라이언트가 보낸 JSON 을 SignupRequest 객체로 변환 -> 바인딩
    public ResponseEntity<ApiResponse<AuthResponse>> signup(@Valid @RequestBody SignupRequest request) {
        AuthResponse response = authService.signup(request);
        return ResponseEntity.ok(ApiResponse.success("회원가입 성공", response)); // HTTP 상태코드 200이랑 응답 바디를 같이 반환
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("로그인 성공", response));
    }

    // 로그아웃
    @PostMapping("/logout")
    // @RequestHeader("Authorization") - 요청 헤더에서 토큰을 꺼냄
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7); // "Bearer " 제거
        String email = jwtUtil.getEmail(token); // 토큰에서 이메일 꺼내서 Redis 키 만드는 데 씀
        authService.logout(email);
        return ResponseEntity.ok(ApiResponse.success("로그아웃 성공", null));
    }

    // 토큰 재발급
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@RequestHeader("Authorization") String authHeader) {
        String refreshedToken = authHeader.substring(7); // "Bearer " 제거
        String email = jwtUtil.getEmail(refreshedToken); // Refresh Token 에서 email 꺼내기
        AuthResponse response = authService.refresh(email);
        return ResponseEntity.ok(ApiResponse.success("토큰 재발급 성공", response));
    }

    // 내 프로필 조회
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MeResponse>> getMe(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MeResponse response = authService.getMe(userDetails.getEmail());
        return ResponseEntity.ok(ApiResponse.success("프로필 조회 성공", response));
    }
}
