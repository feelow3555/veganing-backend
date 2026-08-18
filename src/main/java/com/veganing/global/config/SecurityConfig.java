package com.veganing.global.config;

/*
    Spring Security 전체 설정을 담당하는 클래스 JwtFilter 만들었는데, 이걸 Spring Security 에 등록해줘야 실제로 동작
    * 어떤 경로는 토큰 없어도 되고 (회원가입, 로그인)
    * 어떤 경로는 토큰 있어야 하고 (나머지 API)

    1. JwtFilter 를 필터체인에 등록
        ↓
    2. 경로별 인증 설정
    /api/auth/** → 토큰 없어도 됨 (회원가입, 로그인)
    나머지        → 토큰 있어야 함
        ↓
    3. CORS 설정
    React(5173) → Spring Boot(8080) 통신 허용
*/

import com.veganing.global.auth.JwtFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpMethod;

import java.util.List;

@Configuration // Spring 설정 클래스
@EnableWebSecurity // Spring Security 활성화
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Cross-Site Request Forgery 공격 방지
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Spring Security 한테 "세션 쓰지 마" 선언
                .authorizeHttpRequests(auth -> auth // 경로별 인증 설정
                        .requestMatchers("/api/auth/**").permitAll() // 토큰 없어도 통과
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        // 커뮤니티 조회는 비로그인도 가능
                        .requestMatchers(HttpMethod.GET, "/api/community/posts").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/community/posts/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/challenge/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/carbon/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/product/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/cart/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/recipe/**").permitAll()
                        .anyRequest().authenticated()) // 위에서 설정한 것 외 나머지 모든 요청은 토큰 있어야 함
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class) // Spring Security 필터체인에 JwtFilter 를 등록
                .cors(cors -> cors.configurationSource(corsConfigurationSource()));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of("http://localhost:5173")); // 어떤 출처에서 오는 요청을 허용할지 나중에 배포하면 실제 도메인 추가
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")); // 어떤 HTTP 메서드를 허용할지
        configuration.setAllowedHeaders(List.of("*")); // 어떤 헤더를 허용할지
        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "https://veganing-frontend.vercel.app"
        ));
        configuration.setAllowCredentials(true); // 쿠키나 인증 헤더를 포함한 요청을 허용할지

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // 위에서 만든 CORS 설정을 어떤 경로에 적용할지 등록

        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // 비밀번호 암호화 알고리즘
    }
}
