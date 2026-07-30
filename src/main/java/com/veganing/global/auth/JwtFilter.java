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

        String token = resolveToken(request); // 토큰 꺼냄

        // 토큰이 있는지(로그인, 회원가입은 토큰 필요 x), 유효성 검사
        if(token != null && jwtUtil.isValid(token)) {
            Long userId = jwtUtil.getUserId(token);
            String email = jwtUtil.getEmail(token);

            // Spring Security 한테 "이 사람 인증됐어"라고 알려주는 객체 (email, 비밀번호(토큰 방식이라 필요없어서 null), 이 사람 권한)
            // Role_user -> Spring Security 는 권한 기반으로 접근을 제어해. 나중에 관리자 기능 만들면 ROLE_ADMIN 도 생김
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(email, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
            authenticationToken.setDetails(userId); // userId를 authentication 객체에 추가로 저장 -> 지금 요청한 사람 userId 가 뭐야? 할때 씀

            // SecurityContextHolder -> 현재 요청의 인증 정보 보관함 (여기에 이 유저가 인증된 유저다 저장해 두면 controller 에서 인증확인을 여기서 함)
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        }

        // 필터가 여러개 일때 다른 필터로 엄기고 필터가 다 끝나면 controller 로 감
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