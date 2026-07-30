package com.veganing.global.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey secretKey; // 토큰 서명할때 사용하는 비밀키
    private final long accessTokenExpiry; // 엑세스 토큰 만료시간
    private final long refreshTokenExpiry; // 리프레시 토큰 만료시간

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiry}") long accessTokenExpiry,
            @Value("${jwt.refresh-token-expiry}") long refreshTokenExpiry
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiry = accessTokenExpiry;
        this.refreshTokenExpiry = refreshTokenExpiry;
    }

    // Access Token 생성 메서드
    public String generateAccessToken(Long userId, String email) {
        return Jwts.builder()
                .subject(email) // 토큰 주인이 누군지
                .claim("userId", userId) // 추가로 넣고 싶은 데이터, 나중에 db 조회 없이 userId로 토큰 꺼낼수 있음
                .issuedAt(new Date()) // 토큰 발급 시간
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiry)) // 현재시간 + 15분후 = 만료시간
                .signWith(secretKey) // 사인 (암호화)
                .compact(); // 위 재료들을 합쳐서 문자열 토큰으로 만듬
    }

    // Refresh Token 생성 메서드
    public String generateRefreshToken(String email) {
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiry))
                .signWith(secretKey)
                .compact();
                // Access 와 차이는 .claim("userId", userId) 가 없음 이 사람 정기권 있나 확인 용도라 담을 필요가 없음
                // 사용할 때도 Access 토큰 발급 용도로만 사용
    }

    // claims -> 페이로드
    /*
    subject: "user@gmail.com"
    userId: 1 -> 리프레시 토큰은 userId 없음
    issuedAt: 2026.07.31 10:00
    expiration: 2026.07.31 10:15
    */
    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey) // 사인 확인용
                .build()
                .parseSignedClaims(token) // 토큰 문자열 파싱
                .getPayload(); // 페이로드 꺼내기
    }

    public String getEmail(String token) {
        return getClaims(token).getSubject();
    }

    public Long getUserId(String token) {
        return getClaims(token).get("userId", Long.class);
    }

    // 토큰 유효성 검사
    public boolean isValid(String token) {
        try {
            getClaims(token); // 파싱 성공하면 토큰이 유효하다는거
            return true;
        } catch (Exception e) {
            // 만료된 토큰
            // 위조된 토큰
            // 형식이 잘못된 토큰
            return false;
        }
    }
}
