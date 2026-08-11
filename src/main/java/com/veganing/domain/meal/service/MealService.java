package com.veganing.domain.meal.service;

import com.veganing.domain.auth.entity.User;
import com.veganing.domain.auth.repository.UserRepository;
import com.veganing.domain.challenge.entity.Challenge;
import com.veganing.domain.challenge.repository.ChallengeRepository;
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
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MealService {

    private final MealRepository mealRepository;
    private final UserRepository userRepository;
    private final ChallengeRepository challengeRepository;
    private final MealAsyncService mealAsyncService;
    private final S3Service s3Service;
    private final VoyageAiService voyageAiService;
    private final RecipeRepository recipeRepository;

    @Value("${ai.anthropic.api-key}")
    private String claudeApiKey;

    // Presigned URL 발급 - 프론트가 S3에 직접 업로드할 URL
    public String getUploadUrl(String email) {
        return s3Service.generatePresignedUrl(email);
    }

    // 식단 분석 요청 - ANALYZING row 즉시 생성 후 비동기 분석 시작
    @Transactional
    public Long analyzeMeal(MealAnalyzeRequest request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 진행 중인 챌린지 있으면 연결, 없으면 null
        Challenge challenge = challengeRepository
                .findByUserAndStatus(user, "ONGOING")
                .orElse(null);

        Meal meal = Meal.builder()
                .user(user)
                .challenge(challenge)
                .imageUrl(request.getImageUrl())
                .build();

        Meal savedMeal = mealRepository.save(meal);

        // 외부 클래스 호출 → 프록시 거침 → @Async 정상 동작
        mealAsyncService.processAnalysis(savedMeal.getId(), request, user.getVeganLevel().name());

        return savedMeal.getId();
    }

    // 식단 기록 목록 조회
    @Transactional(readOnly = true)
    public List<MealResponse> getHistory(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return mealRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(MealResponse::from)
                .toList();
    }

    // 단건 조회 - 프론트 폴링용 (ANALYZING → DONE/FAILED 확인)
    @Transactional(readOnly = true)
    public MealResponse getMeal(Long mealId, String email) {
        Meal meal = mealRepository.findById(mealId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEAL_NOT_FOUND));

        // 본인 식단만 조회 가능
        if (!meal.getUser().getEmail().equals(email)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        return MealResponse.from(meal);
    }

    // RAG 기반 식단 추천
// 1. 최근 완료 식단 조회 → 2. 쿼리 벡터 생성 → 3. 유사 레시피 검색 → 4. Claude 추천 생성
    @Transactional(readOnly = true)
    public RecommendResponse recommend(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 가장 최근 완료된 식단 조회
        Meal meal = mealRepository
                .findFirstByUserAndStatusOrderByCreatedAtDesc(user, MealStatus.DONE)
                .orElseThrow(() -> new CustomException(ErrorCode.MEAL_NOT_FOUND));

        // 식단 피드백 텍스트로 쿼리 벡터 생성
        // aiFeedback = 부족 영양소 + 비건 관점 피드백 → 이걸 임베딩해서 유사 레시피 검색
        float[] queryVector = voyageAiService.embed(meal.getAiFeedback());

        // float[] → "[0.1,0.2,...]" 문자열 변환 (네이티브 쿼리 CAST용)
        String queryVectorStr = Arrays.toString(queryVector).replace(" ", "");

        // pgvector 코사인 유사도 검색 → 유사 레시피 Top 3
        List<Recipe> similarRecipes = recipeRepository.findSimilarRecipes(queryVectorStr, 3);

        if (similarRecipes.isEmpty()) {
            return RecommendResponse.builder()
                    .recommendation("아직 추천할 레시피가 없어요. 커뮤니티에 레시피를 공유해주세요!")
                    .referenceRecipes(List.of())
                    .build();
        }

        // 검색된 레시피를 Claude 프롬프트 컨텍스트로 구성
        String recipeContext = similarRecipes.stream()
                .map(r -> "제목: " + r.getTitle() + "\n내용: " + r.getContent())
                .collect(Collectors.joining("\n\n"));

        // Claude API 호출 (텍스트만, Vision 아님)
        RestClient restClient = RestClient.builder()
                .baseUrl("https://api.anthropic.com")
                .build();

        String prompt = String.format("""
            사용자의 최근 식단 분석 결과:
            %s
            
            커뮤니티에서 인기 있는 비건 레시피:
            %s
            
            위 레시피를 참고해서 사용자에게 맞는 식단을 3~5문장으로 추천해줘.
            부족한 영양소를 보완할 수 있는 방향으로 추천하고, 한국어로 친근하게 작성해줘.
            """, meal.getAiFeedback(), recipeContext);

        Map response = restClient.post()
                .uri("/v1/messages")
                .header("x-api-key", claudeApiKey)
                .header("anthropic-version", "2023-06-01")
                .header("Content-Type", "application/json")
                .body(Map.of(
                        "model", "claude-sonnet-4-5",
                        "max_tokens", 1024,
                        "messages", List.of(Map.of("role", "user", "content", prompt))
                ))
                .retrieve()
                .body(Map.class);

        // Claude 응답에서 텍스트 추출
        List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
        String recommendation = (String) content.get(0).get("text");

        return RecommendResponse.builder()
                .recommendation(recommendation)
                .referenceRecipes(similarRecipes.stream().map(Recipe::getTitle).toList())
                .build();
    }
}