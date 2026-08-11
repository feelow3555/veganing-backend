package com.veganing.global.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

// JUnit5 단위테스트 클래스
// @SpringBootTest 없음 = Spring 컨텍스트 안 띄움 = 빠름
class JwtUtilTest {

    private JwtUtil jwtUtil;

    // 테스트용 상수 - 실제 application-local.yaml 값 안 씀
    // 단위테스트는 외부 설정 파일에 의존하면 안 됨
    private static final String SECRET = "test-secret-key-must-be-at-least-32-characters-long";
    private static final long ACCESS_EXPIRY = 1000 * 60 * 15;           // 15분 (ms)
    private static final long REFRESH_EXPIRY = 1000 * 60 * 60 * 24 * 7; // 7일 (ms)
    private static final Long USER_ID = 1L;
    private static final String EMAIL = "test@test.com";

    // @BeforeEach: 각 @Test 메서드 실행 전마다 호출됨
    // 테스트마다 새 객체를 만들어서 테스트 간 상태 공유를 막음
    // ex) 테스트A에서 jwtUtil 상태가 변해도 테스트B에 영향 없음
    @BeforeEach
    void setUp() {
        // Spring @Value 주입 없이 생성자로 직접 값 주입
        // 단위테스트는 Spring 없이 순수 Java 객체만으로 동작해야 함
        jwtUtil = new JwtUtil(SECRET, ACCESS_EXPIRY, REFRESH_EXPIRY);
    }

    // @Test: JUnit이 이 메서드를 테스트로 인식하고 실행함
    // @DisplayName: 테스트 결과 화면에 표시될 한글 설명
    @Test
    @DisplayName("Access Token 생성 시 JWT 형식 문자열을 반환한다")
    void generateAccessToken_success() {
        // given: 테스트에 필요한 데이터 준비
        // USER_ID = 1L, EMAIL = "test@test.com" 상수 사용

        // when: 테스트할 메서드 실행
        String token = jwtUtil.generateAccessToken(USER_ID, EMAIL);

        // then: 결과 검증
        // assertThat(): AssertJ 검증 메서드, null 체크 + JWT 형식(헤더.페이로드.서명) 확인
        assertThat(token).isNotNull();
        assertThat(token.split("\\.")).hasSize(3); // JWT = header.payload.signature 3파트
    }

    @Test
    @DisplayName("Refresh Token 생성 시 JWT 형식 문자열을 반환한다")
    void generateRefreshToken_success() {
        // given: EMAIL만 있으면 됨 (Refresh Token은 userId 안 담음)

        // when
        String token = jwtUtil.generateRefreshToken(EMAIL);

        // then
        assertThat(token).isNotNull();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("Access Token에서 이메일을 추출한다")
    void getEmail_success() {
        // given: 토큰 먼저 생성
        String token = jwtUtil.generateAccessToken(USER_ID, EMAIL);

        // when: 토큰에서 이메일 추출
        String extractedEmail = jwtUtil.getEmail(token);

        // then: 넣은 이메일과 꺼낸 이메일이 같아야 함
        assertThat(extractedEmail).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("Access Token에서 userId를 추출한다")
    void getUserId_success() {
        // given
        String token = jwtUtil.generateAccessToken(USER_ID, EMAIL);

        // when
        Long extractedUserId = jwtUtil.getUserId(token);

        // then: 넣은 userId와 꺼낸 userId가 같아야 함
        assertThat(extractedUserId).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("유효한 토큰은 isValid가 true를 반환한다")
    void isValid_validToken_returnsTrue() {
        // given: 정상 토큰 생성
        String token = jwtUtil.generateAccessToken(USER_ID, EMAIL);

        // when
        boolean result = jwtUtil.isValid(token);

        // then: 유효한 토큰이므로 true
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("만료된 토큰은 isValid가 false를 반환한다")
    void isValid_expiredToken_returnsFalse() {
        // given: 만료시간을 0ms로 설정한 JwtUtil 별도 생성
        // 0ms = 생성 즉시 만료
        // 기존 jwtUtil(15분)을 쓰면 만료 테스트가 불가능해서 별도 객체 사용
        JwtUtil expiredJwtUtil = new JwtUtil(SECRET, 0L, REFRESH_EXPIRY);
        String token = expiredJwtUtil.generateAccessToken(USER_ID, EMAIL);

        // when: 기존 jwtUtil로 검증 (만료된 토큰이라 false 나와야 함)
        boolean result = jwtUtil.isValid(token);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("위조된 토큰은 isValid가 false를 반환한다")
    void isValid_tamperedToken_returnsFalse() {
        // given: 정상 토큰 생성 후 뒷부분 변조
        // 서명(Signature) 부분이 달라지면 위조 감지됨
        String token = jwtUtil.generateAccessToken(USER_ID, EMAIL);
        String tamperedToken = token + "tampered";

        // when
        boolean result = jwtUtil.isValid(tamperedToken);

        // then: 위조된 토큰이므로 false
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Refresh Token에는 userId가 없어서 null을 반환한다")
    void getUserId_refreshToken_returnsNull() {
        // given: Refresh Token은 userId claim을 담지 않음
        String refreshToken = jwtUtil.generateRefreshToken(EMAIL);

        // when
        Long userId = jwtUtil.getUserId(refreshToken);

        // then: claim이 없으면 null 반환
        assertThat(userId).isNull();
    }
}