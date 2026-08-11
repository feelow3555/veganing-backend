package com.veganing.domain.meal.service;

import com.veganing.domain.auth.entity.User;
import com.veganing.domain.auth.repository.UserRepository;
import com.veganing.domain.challenge.entity.Challenge;
import com.veganing.domain.challenge.repository.ChallengeRepository;
import com.veganing.domain.challenge.entity.ChallengeType;
import com.veganing.domain.meal.dto.MealAnalyzeRequest;
import com.veganing.domain.meal.dto.MealResponse;
import com.veganing.domain.meal.entity.Meal;
import com.veganing.domain.meal.repository.MealRepository;
import com.veganing.global.error.CustomException;
import com.veganing.global.error.ErrorCode;
import com.veganing.global.infra.s3.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.doReturn;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

// @ExtendWith(MockitoExtension.class)
// JUnit5에서 Mockito를 사용하기 위한 설정
// @SpringBootTest 없이 Mock 객체만으로 테스트 가능하게 해줌
@ExtendWith(MockitoExtension.class)
class MealServiceTest {

    // @InjectMocks: 테스트 대상 클래스
    // @Mock으로 만든 가짜 객체들을 자동으로 주입해줌
    @InjectMocks
    private MealService mealService;

    // @Mock: 가짜 객체 생성
    // 실제 DB, S3 안 쓰고 동작을 흉내내는 가짜
    @Mock private MealRepository mealRepository;
    @Mock private UserRepository userRepository;
    @Mock private ChallengeRepository challengeRepository;
    @Mock private MealAsyncService mealAsyncService;
    @Mock private S3Service s3Service;

    private User mockUser;
    private Meal mockMeal;
    private MealAnalyzeRequest mockRequest;

    @BeforeEach
    void setUp() {
        // 테스트용 User 객체 (DB 저장 안 함, 메모리에만 존재)
        mockUser = User.builder()
                .email("test@test.com")
                .veganLevel(ChallengeType.VEGAN)
                .build();

        // 테스트용 Meal 객체
        // Meal.builder() 대신 spy() 사용
        mockMeal = Meal.builder()
                .user(mockUser)
                .imageUrl("https://s3.amazonaws.com/test/meal.jpg")
                .build();

        // 테스트용 요청 DTO
        mockRequest = MealAnalyzeRequest.builder()
                .imageUrl("https://s3.amazonaws.com/test/meal.jpg")
                .foodName("비건 샐러드")
                .foodDescription("신선한 채소 샐러드")  // description → foodDescription
                .build();
    }

    @Test
    @DisplayName("Presigned URL 발급 요청 시 S3Service에서 URL을 반환한다")
    void getUploadUrl_success() {
        // given
        // given(): Mock 객체의 동작 정의
        // "s3Service.generatePresignedUrl() 호출되면 이 값을 반환해라"
        String expectedUrl = "https://s3.amazonaws.com/presigned-url";
        given(s3Service.generatePresignedUrl(any())).willReturn(expectedUrl);

        // when
        String result = mealService.getUploadUrl("test@test.com");

        // then
        assertThat(result).isEqualTo(expectedUrl);
        // verify(): Mock 메서드가 실제로 호출됐는지 검증
        then(s3Service).should().generatePresignedUrl(any());
    }

    @Test
    @DisplayName("식단 분석 요청 시 ANALYZING 상태의 Meal을 생성하고 mealId를 반환한다")
    void analyzeMeal_success() {
        // given
        Meal spyMeal = spy(mockMeal);
        doReturn(1L).when(spyMeal).getId();

        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(mockUser));
        given(challengeRepository.findByUserAndStatus(any(), any())).willReturn(Optional.empty());
        given(mealRepository.save(any(Meal.class))).willReturn(spyMeal); // spyMeal 반환

        // when
        Long mealId = mealService.analyzeMeal(mockRequest, "test@test.com");

        // then
        assertThat(mealId).isNotNull();
        then(mealRepository).should().save(any(Meal.class));
        then(mealAsyncService).should().processAnalysis(any(), any(), any());
    }

    @Test
    @DisplayName("식단 분석 요청 시 진행중인 챌린지가 있으면 연결된다")
    void analyzeMeal_withChallenge() {
        // given
        Meal spyMeal = spy(mockMeal);
        doReturn(1L).when(spyMeal).getId();

        Challenge mockChallenge = Challenge.builder()
                .user(mockUser)
                .status("ONGOING")
                .build();

        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(mockUser));
        given(challengeRepository.findByUserAndStatus(any(), any())).willReturn(Optional.of(mockChallenge));
        given(mealRepository.save(any(Meal.class))).willReturn(spyMeal);

        // when
        Long mealId = mealService.analyzeMeal(mockRequest, "test@test.com");

        // then
        assertThat(mealId).isNotNull();
        then(mealRepository).should().save(any(Meal.class));
    }

    @Test
    @DisplayName("존재하지 않는 유저로 분석 요청 시 CustomException이 발생한다")
    void analyzeMeal_userNotFound() {
        // given
        given(userRepository.findByEmail(any())).willReturn(Optional.empty());

        // when & then
        // assertThatThrownBy(): 예외 발생 검증
        assertThatThrownBy(() -> mealService.analyzeMeal(mockRequest, "none@test.com"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("식단 목록 조회 시 유저의 전체 식단을 최신순으로 반환한다")
    void getHistory_success() {
        // given
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(mockUser));
        given(mealRepository.findByUserOrderByCreatedAtDesc(mockUser)).willReturn(List.of(mockMeal));

        // when
        List<MealResponse> result = mealService.getHistory("test@test.com");

        // then
        assertThat(result).hasSize(1);
        then(mealRepository).should().findByUserOrderByCreatedAtDesc(mockUser);
    }

    @Test
    @DisplayName("단건 조회 시 본인 식단이면 정상 반환한다")
    void getMeal_success() {
        // given
        given(mealRepository.findById(1L)).willReturn(Optional.of(mockMeal));

        // when
        MealResponse result = mealService.getMeal(1L, "test@test.com");

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("단건 조회 시 타인 식단이면 FORBIDDEN 예외가 발생한다")
    void getMeal_forbidden() {
        // given
        given(mealRepository.findById(1L)).willReturn(Optional.of(mockMeal));

        // when & then
        // mockMeal의 user email은 "test@test.com"인데 다른 이메일로 접근
        assertThatThrownBy(() -> mealService.getMeal(1L, "other@test.com"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("존재하지 않는 식단 조회 시 MEAL_NOT_FOUND 예외가 발생한다")
    void getMeal_notFound() {
        // given
        given(mealRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> mealService.getMeal(999L, "test@test.com"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEAL_NOT_FOUND);
    }
}