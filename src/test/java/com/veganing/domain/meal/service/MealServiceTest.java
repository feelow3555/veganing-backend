package com.veganing.domain.meal.service;

import com.veganing.domain.auth.entity.User;
import com.veganing.domain.auth.repository.UserRepository;
import com.veganing.domain.challenge.entity.Challenge;
import com.veganing.domain.challenge.repository.ChallengeRepository;
import com.veganing.domain.challenge.entity.ChallengeType;
import com.veganing.domain.meal.dto.MealAnalyzeRequest;
import com.veganing.domain.meal.dto.MealResponse;
import com.veganing.domain.meal.dto.RecommendResponse;
import com.veganing.domain.meal.entity.Meal;
import com.veganing.domain.meal.enums.MealStatus;
import com.veganing.domain.meal.repository.MealRepository;
import com.veganing.domain.recipe.entity.Recipe;
import com.veganing.domain.recipe.repository.RecipeRepository;
import com.veganing.global.error.CustomException;
import com.veganing.global.error.ErrorCode;
import com.veganing.global.infra.s3.S3Service;
import com.veganing.global.infra.voyage.VoyageAiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MealServiceTest {

    @InjectMocks
    private MealService mealService;

    // Phase 7에서 추가된 의존성 두 개도 Mock으로 추가
    // 없으면 @InjectMocks 실패 (MealService 생성자에서 주입 못 찾음)
    @Mock private MealRepository mealRepository;
    @Mock private UserRepository userRepository;
    @Mock private ChallengeRepository challengeRepository;
    @Mock private MealAsyncService mealAsyncService;
    @Mock private S3Service s3Service;
    @Mock private VoyageAiService voyageAiService;  // Phase 7 추가
    @Mock private RecipeRepository recipeRepository; // Phase 7 추가

    private User mockUser;
    private Meal mockMeal;
    private MealAnalyzeRequest mockRequest;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .email("test@test.com")
                .veganLevel(ChallengeType.VEGAN)
                .build();

        mockMeal = Meal.builder()
                .user(mockUser)
                .imageUrl("https://s3.amazonaws.com/test/meal.jpg")
                .build();

        mockRequest = MealAnalyzeRequest.builder()
                .imageUrl("https://s3.amazonaws.com/test/meal.jpg")
                .foodName("비건 샐러드")
                .foodDescription("신선한 채소 샐러드")
                .build();

        // MealService의 @Value("${ai.anthropic.api-key}") claudeApiKey 주입
        // @Value는 Spring 컨텍스트(ApplicationContext)가 있어야 주입됨
        // @SpringBootTest 없이 순수 Mockito 테스트이므로 컨텍스트가 없음
        // → ReflectionTestUtils로 private 필드에 직접 값을 넣어 해결
        ReflectionTestUtils.setField(mealService, "claudeApiKey", "test-claude-api-key");
    }

    // ────────────────────────────────────────────
    // Presigned URL 발급
    // ────────────────────────────────────────────

    @Test
    @DisplayName("Presigned URL 발급 요청 시 S3Service에서 URL을 반환한다")
    void getUploadUrl_success() {
        // given
        String expectedUrl = "https://s3.amazonaws.com/presigned-url";
        given(s3Service.generatePresignedUrl(any())).willReturn(expectedUrl);

        // when
        String result = mealService.getUploadUrl("test@test.com");

        // then
        assertThat(result).isEqualTo(expectedUrl);
        // S3Service가 실제로 호출됐는지도 검증
        then(s3Service).should().generatePresignedUrl(any());
    }

    // ────────────────────────────────────────────
    // 식단 분석 요청 (analyzeMeal)
    // ────────────────────────────────────────────

    @Test
    @DisplayName("식단 분석 요청 시 ANALYZING 상태의 Meal을 생성하고 mealId를 반환한다")
    void analyzeMeal_success() {
        // given
        // @GeneratedValue id는 DB가 채워주는 값 → builder()로 직접 세팅 불가
        // spy()로 실제 객체를 감싸고 getId()만 오버라이드해서 1L을 반환하게 함
        // spy vs mock: mock은 모든 메서드가 기본값 반환, spy는 실제 동작 유지 + 일부만 오버라이드
        Meal spyMeal = spy(mockMeal);
        doReturn(1L).when(spyMeal).getId();

        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(mockUser));
        // 진행 중인 챌린지 없음 → challenge = null로 Meal 생성
        given(challengeRepository.findByUserAndStatus(any(), any())).willReturn(Optional.empty());
        given(mealRepository.save(any(Meal.class))).willReturn(spyMeal);

        // when
        Long mealId = mealService.analyzeMeal(mockRequest, "test@test.com");

        // then
        assertThat(mealId).isNotNull();
        // Meal이 DB에 저장됐는지 검증
        then(mealRepository).should().save(any(Meal.class));
        // 비동기 분석이 시작됐는지 검증 (실제 AI 호출은 MealAsyncService 내부)
        then(mealAsyncService).should().processAnalysis(any(), any(), any());
    }

    @Test
    @DisplayName("식단 분석 요청 시 진행중인 챌린지가 있으면 Meal에 연결된다")
    void analyzeMeal_withChallenge() {
        // given
        Meal spyMeal = spy(mockMeal);
        doReturn(1L).when(spyMeal).getId();

        Challenge mockChallenge = Challenge.builder()
                .user(mockUser)
                .status("ONGOING")
                .build();

        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(mockUser));
        // 진행 중인 챌린지 존재 → Meal.challenge = mockChallenge로 연결돼야 함
        given(challengeRepository.findByUserAndStatus(any(), any())).willReturn(Optional.of(mockChallenge));
        given(mealRepository.save(any(Meal.class))).willReturn(spyMeal);

        // when
        Long mealId = mealService.analyzeMeal(mockRequest, "test@test.com");

        // then
        assertThat(mealId).isNotNull();
        then(mealRepository).should().save(any(Meal.class));
    }

    @Test
    @DisplayName("존재하지 않는 유저로 분석 요청 시 USER_NOT_FOUND 예외가 발생한다")
    void analyzeMeal_userNotFound() {
        // given
        // findByEmail이 empty → orElseThrow → CustomException(USER_NOT_FOUND)
        given(userRepository.findByEmail(any())).willReturn(Optional.empty());

        // when & then
        // hasFieldOrPropertyWithValue: 예외 객체의 errorCode 필드값 검증
        assertThatThrownBy(() -> mealService.analyzeMeal(mockRequest, "none@test.com"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    // ────────────────────────────────────────────
    // 식단 목록 조회 (getHistory)
    // ────────────────────────────────────────────

    @Test
    @DisplayName("식단 목록 조회 시 유저의 전체 식단을 최신순으로 반환한다")
    void getHistory_success() {
        // given
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(mockUser));
        // findByUserOrderByCreatedAtDesc: 최신순 정렬 쿼리 메서드
        given(mealRepository.findByUserOrderByCreatedAtDesc(mockUser)).willReturn(List.of(mockMeal));

        // when
        List<MealResponse> result = mealService.getHistory("test@test.com");

        // then
        assertThat(result).hasSize(1);
        // 정렬 메서드가 실제로 호출됐는지 검증
        then(mealRepository).should().findByUserOrderByCreatedAtDesc(mockUser);
    }

    // ────────────────────────────────────────────
    // 식단 단건 조회 (getMeal) - 폴링용
    // ────────────────────────────────────────────

    @Test
    @DisplayName("단건 조회 시 본인 식단이면 정상 반환한다")
    void getMeal_success() {
        // given
        // mockMeal.user.email = "test@test.com" → 같은 email로 조회하면 통과
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
        // mockMeal의 user email은 "test@test.com"
        // "other@test.com"으로 접근 → email 불일치 → FORBIDDEN
        given(mealRepository.findById(1L)).willReturn(Optional.of(mockMeal));

        // when & then
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

    // ────────────────────────────────────────────
    // RAG 기반 식단 추천 (recommend)
    //
    // 흐름: 최근 완료 식단 조회 → aiFeedback 임베딩 → 유사 레시피 검색 → Claude 추천 생성
    //
    // 한계: recommend() 내부에서 RestClient를 직접 new로 생성해 Claude API 호출
    //       → Mock으로 Claude 호출을 끊을 수 없음
    //       → "레시피 없을 때 기본 응답 반환"처럼 Claude 호출 전에 return하는 케이스만 완전 검증 가능
    //       → "레시피 있을 때 Claude 호출" 케이스는 Claude 이전까지의 로직만 검증
    //       → Claude 응답 파싱까지 포함한 e2e 검증은 Phase 8 통합테스트에서 처리
    //
    // 리팩토링 포인트 (Phase 8):
    //       recommend() 안의 Claude 호출을 ClaudeAiService 별도 클래스로 분리하면
    //       @Mock으로 주입해 완전한 단위테스트 가능
    // ────────────────────────────────────────────

    @Test
    @DisplayName("완료된 식단이 없으면 MEAL_NOT_FOUND 예외가 발생한다")
    void recommend_noCompletedMeal_throwsException() {
        // given
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(mockUser));
        // MealStatus.DONE인 식단이 없음
        // → orElseThrow → CustomException(MEAL_NOT_FOUND)
        given(mealRepository.findFirstByUserAndStatusOrderByCreatedAtDesc(mockUser, MealStatus.DONE))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> mealService.recommend("test@test.com"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEAL_NOT_FOUND);

        // 식단이 없으면 임베딩 요청도 발생하면 안 됨 (불필요한 API 비용 방지)
        then(voyageAiService).should(never()).embed(any());
    }

    @Test
    @DisplayName("유사 레시피가 없으면 기본 안내 메시지를 반환한다")
    void recommend_noSimilarRecipes_returnsDefault() {
        // given
        // aiFeedback이 있는 완료된 식단 (Voyage AI 임베딩의 입력값)
        Meal doneMeal = Meal.builder()
                .user(mockUser)
                .imageUrl("https://s3.amazonaws.com/test/meal.jpg")
                .aiFeedback("비타민 B12가 부족합니다. 두유나 영양 효모를 추가해보세요.")
                .build();

        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(mockUser));
        given(mealRepository.findFirstByUserAndStatusOrderByCreatedAtDesc(mockUser, MealStatus.DONE))
                .willReturn(Optional.of(doneMeal));
        // aiFeedback 텍스트를 임베딩해서 쿼리 벡터 생성
        given(voyageAiService.embed(any())).willReturn(new float[]{0.1f, 0.2f, 0.3f});
        // pgvector 검색 결과 없음 → 레시피 DB가 비어있거나 유사도 임계값 미달
        given(recipeRepository.findSimilarRecipes(any(), eq(3))).willReturn(List.of());

        // when
        RecommendResponse result = mealService.recommend("test@test.com");

        // then
        // 레시피 없을 때 Claude 호출 없이 바로 기본 메시지 반환
        // → 이 케이스는 Claude 호출 전에 return하므로 완전 검증 가능
        assertThat(result.getRecommendation()).contains("아직 추천할 레시피가 없어요");
        assertThat(result.getReferenceRecipes()).isEmpty();
    }

    @Test
    @DisplayName("유사 레시피가 있으면 Voyage 임베딩과 레시피 검색이 수행된다")
    void recommend_success() {
        // given
        Meal doneMeal = Meal.builder()
                .user(mockUser)
                .imageUrl("https://s3.amazonaws.com/test/meal.jpg")
                .aiFeedback("오메가3가 부족합니다. 아마씨나 호두를 추가해보세요.")
                .build();

        Recipe mockRecipe = Recipe.builder()
                .title("아마씨 스무디 볼")
                .content("아마씨, 바나나, 아몬드밀크를 블렌딩해주세요")
                .build();

        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(mockUser));
        given(mealRepository.findFirstByUserAndStatusOrderByCreatedAtDesc(mockUser, MealStatus.DONE))
                .willReturn(Optional.of(doneMeal));
        given(voyageAiService.embed(any())).willReturn(new float[]{0.1f, 0.2f, 0.3f});
        given(recipeRepository.findSimilarRecipes(any(), eq(3))).willReturn(List.of(mockRecipe));

        // when & then
        // 이 시점 이후 MealService 내부에서 RestClient를 new로 생성해 Claude API를 직접 호출함
        // 테스트 환경에서는 실제 Claude API 서버에 연결을 시도하다 실패 → 예외 발생
        // CustomException이 아닌 것만 확인 (네트워크/API 에러이므로 비즈니스 로직 에러가 아님)
        assertThatThrownBy(() -> mealService.recommend("test@test.com"))
                .isNotInstanceOf(CustomException.class);

        // Claude 호출 이전 로직은 정상 수행됐는지 검증
        // aiFeedback 텍스트로 embed가 호출됐는지 확인
        then(voyageAiService).should().embed(doneMeal.getAiFeedback());
        // Top 3 유사 레시피 검색이 호출됐는지 확인
        then(recipeRepository).should().findSimilarRecipes(any(), eq(3));
    }
}