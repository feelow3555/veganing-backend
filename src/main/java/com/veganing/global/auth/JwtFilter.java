package com.veganing.global.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
/*
    JwtFilter = 모든 API 요청이 Controller 에 도달하기 전에 가로채서 토큰 검사하는 역할
*/

@Component
// OncePerRequestFilter → Spring 이 제공하는 필터 클래스 / 이걸 상속받으면 요청당 딱 한 번만 실행되는 필터를 만듬
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil; // 토큰 검사할 때 JwtUtil 메서드들을 쓸 거라서 의존성 주입
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request); // 요청 헤더에서 토큰 꺼냄

        // 토큰이 존재하고 유효한 경우에만 인증 처리
        // 로그인/회원가입 요청은 토큰이 없으므로 이 블록 건너뜀
        if(token != null && jwtUtil.isValid(token)) {
            Long userId = jwtUtil.getUserId(token);
            String email = jwtUtil.getEmail(token);

            // 토큰에서 꺼낸 userId + email 로 인증 객체 생성
            // UserDetails 구현체로 감싸야 @AuthenticationPrincipal 로 Controller 에서 꺼낼 수 있음
            CustomUserDetails userDetails = new CustomUserDetails(userId, email);

            // Spring Security 에 "이 사람 인증됐어" 라고 알려주는 객체
            // principal 자리에 CustomUserDetails 를 넣어야 Controller 에서 타입 일치로 꺼낼 수 있음
            // 비밀번호는 토큰 방식이라 불필요 → null
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            // 현재 요청의 인증 정보 보관함에 저장
            // 이후 Controller 에서 @AuthenticationPrincipal 로 꺼내 씀
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        }

        // 다음 필터로 넘김. 필터 체인이 끝나면 Controller 로 전달됨
        filterChain.doFilter(request, response);
    }

    // Bearer 을 제거하는 함수
    // Bearer -> HTTP 인증 방식 표준 / "이 토큰을 가진 사람(Bearer)에게 접근을 허용해라" 라는 의미 JWT 쓸 때 표준으로 Bearer 를 붙임
    private String resolveToken(HttpServletRequest request) {
        // 요청 헤더에서 Authorization 값을 꺼냄
        // Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
        String bearer = request.getHeader("Authorization");

        // "Bearer "로 시작하는지 확인해. 형식이 맞는지 검증, "Bearer " 7글자 잘라내고 순수 토큰만 꺼냄
        if(bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        // 토큰 없는 요청이면 null 반환
        return null;
    }
}