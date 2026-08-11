package com.veganing.domain.meal.service;

import com.veganing.domain.auth.entity.User;
import com.veganing.domain.auth.repository.UserRepository;
import com.veganing.domain.challenge.entity.Challenge;
import com.veganing.domain.challenge.repository.ChallengeRepository;
import com.veganing.domain.meal.dto.MealAnalyzeRequest;
import com.veganing.domain.meal.dto.MealResponse;
import com.veganing.domain.meal.entity.Meal;
import com.veganing.domain.meal.repository.MealRepository;
import com.veganing.global.error.CustomException;
import com.veganing.global.error.ErrorCode;
import com.veganing.global.infra.s3.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MealService {

    private final MealRepository mealRepository;
    private final UserRepository userRepository;
    private final ChallengeRepository challengeRepository;
    private final MealAsyncService mealAsyncService;
    private final S3Service s3Service;

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
}