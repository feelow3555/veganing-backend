package com.veganing.domain.auth.service;

import com.veganing.domain.auth.dto.AuthResponse;
import com.veganing.domain.auth.dto.LoginRequest;
import com.veganing.domain.auth.dto.MeResponse;
import com.veganing.domain.auth.dto.SignupRequest;
import com.veganing.domain.auth.entity.User;
import com.veganing.domain.auth.repository.UserRepository;
import com.veganing.domain.cart.entity.Cart;
import com.veganing.domain.cart.repository.CartRepository;
import com.veganing.global.auth.JwtUtil;
import com.veganing.global.error.CustomException;
import com.veganing.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor // Lombok 이 final 필드들을 받는 생성자를 자동으로 만들어줌 나중에 의존성 주입할 때 사용
public class AuthService {

    private final UserRepository userRepository; // DB 에서 유저 조회/저장
    private final PasswordEncoder passwordEncoder; // 비밀번호 암호화/검증
    private final JwtUtil jwtUtil; // 토큰 생성
    private final RedisTemplate<String, String> redisTemplate; // Refresh Token 저장
    private final CartRepository cartRepository; // 회원가입 시 장바구니 생성

    // 회원가입 메서드
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        // 1. 이메일 중복 확인
        if(userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        // 2. 비밀번호 암호화 + 유저 저장
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword())) // 비밀번호를 암호화해서 저장
                .nickname(request.getNickname())
                .region(request.getRegion())
                .build();

        // 3. 토큰 발급 + 반환
        User savedUser = userRepository.save(user);

        // 회원가입 시 Cart 자동 생성
        cartRepository.save(Cart.builder()
                .user(savedUser)
                .build());

        String accessToken = jwtUtil.generateAccessToken(savedUser.getId(), savedUser.getEmail()); // jwtUtil 로 Access Token 생성

        return AuthResponse.of(accessToken); // DTO 에 토큰 담아서 반환하면 Controller 가 받아서 클라이언트한테 내려줌
    }

    // 로그인 메서드
    @Transactional
    public AuthResponse login(LoginRequest request) {

        // 1. 이메일로 유저 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("존재하지 않는 이메일입니다."));

        // 2. 비밀번호 확인
        // passwordEncoder.matches() - 클라이언트가 보낸 평문 비밀번호랑 DB에 암호화된 비밀번호를 비교해줌
        // 직접 비교 못 하는 이유가 BCrypt 는 같은 비밀번호도 암호화할 때마다 다른 값이 나옴
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        // 3. 토큰 발급
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        // 4. Refresh Token Redis 에 저장
        redisTemplate.opsForValue().set(
                "refresh:" + user.getEmail(), //키:   "refresh:a@a.com"   → 나중에 이 키로 꺼내거나 삭제
                refreshToken, // 값:   refreshToken 문자열
                7, TimeUnit.DAYS // TTL:  7일 후 자동 삭제
        );

        return AuthResponse.of(accessToken);
    }

    // 로그아웃 메서드
    @Transactional
    public void logout(String email) {
        redisTemplate.delete("refresh:" + email); // redis 에서 refresh 토큰 삭제 하면 로그아웃
    }

    // 토큰 재발급 메서드 -> 토큰 만료시 사용
    @Transactional
    public AuthResponse refresh(String email) {

        // 1. Redis 에서 Refresh Token 확인
        String refreshToken = redisTemplate.opsForValue().get("refresh:" + email);

        // 2. 없으면 예외 (로그아웃된 상태)
        if (refreshToken == null) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        // 3. 새 Access Token 발급
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail());

        return AuthResponse.of(newAccessToken);
    }

    // 내 프로필 조회
    public MeResponse getMe(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return MeResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .region(user.getRegion())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
