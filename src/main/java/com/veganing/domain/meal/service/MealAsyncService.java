// MealAsyncService.java
package com.veganing.domain.meal.service;

import com.veganing.domain.carbon.entity.CarbonDaily;
import com.veganing.domain.carbon.repository.CarbonDailyRepository;
import com.veganing.domain.ingredient.entity.Ingredient;
import com.veganing.domain.ingredient.repository.IngredientRepository;
import com.veganing.domain.meal.dto.MealAnalyzeRequest;
import com.veganing.domain.meal.dto.MealIngredient;
import com.veganing.domain.meal.entity.Meal;
import com.veganing.domain.meal.repository.MealRepository;
import com.veganing.global.error.CustomException;
import com.veganing.global.error.ErrorCode;
import com.veganing.global.infra.vision.VisionAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MealAsyncService {

    private final MealRepository mealRepository;
    private final IngredientRepository ingredientRepository;
    private final CarbonDailyRepository carbonDailyRepository;
    private final VisionAiService visionAiService;

    private static final BigDecimal KOREAN_DAILY_AVG_CARBON = new BigDecimal("5.5");

    // 외부 클래스(MealService)에서 호출 → 프록시 거침 → @Async 정상 동작
    @Async
    @Transactional
    public void processAnalysis(Long mealId, MealAnalyzeRequest request, String veganLevel) {
        log.info("=== 비동기 분석 시작 mealId={} ===", mealId);
        Meal meal = mealRepository.findById(mealId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEAL_NOT_FOUND));

        log.info("=== meal 조회 완료 ===");
        try {
            log.info("=== Vision AI 호출 시작 ===");
            // 1단계: Vision AI 호출
            Map<String, Object> aiResult = visionAiService.analyzeMeal(
                    meal.getImageUrl(),
                    request.getFoodName(),
                    request.getFoodDescription(),
                    veganLevel
            );

            // 2단계: 식재료명 → DB 조회 → 수치 스냅샷 생성
            List<Map<String, Object>> rawIngredients =
                    (List<Map<String, Object>>) aiResult.get("ingredients");
            List<MealIngredient> mealIngredients = buildMealIngredients(rawIngredients);

            // 3단계: 영양소 합산
            Map<String, BigDecimal> nutrition = calculateNutrition(mealIngredients);

            // 4단계: 탄소 발자국 합산
            BigDecimal totalCarbon = mealIngredients.stream()
                    .map(mi -> nullSafe(mi.getCo2()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 5단계: 분석 결과 저장
            List<Map<String, Object>> rawSuspected =
                    (List<Map<String, Object>>) aiResult.get("suspected_violations");
            List<Meal.SuspectedViolation> suspectedViolations = rawSuspected == null
                    ? new ArrayList<>()
                    : rawSuspected.stream()
                    .map(m -> Meal.SuspectedViolation.builder()
                            .ingredient((String) m.get("ingredient"))
                            .reason((String) m.get("reason"))
                            .confidence(((Number) m.get("confidence")).doubleValue())
                            .build())
                    .toList();

            meal.completeAnalysis(
                    mealIngredients,
                    totalCarbon,
                    nutrition,
                    (String) aiResult.get("nutrition_feedback"),
                    (Boolean) aiResult.get("is_vegan_compliant"),
                    (List<String>) aiResult.get("confirmed_violations"),
                    suspectedViolations
            );

            // 6단계: carbon_daily upsert
            updateCarbonDaily(meal, totalCarbon);

        } catch (Exception e) {
            log.error("식단 분석 실패 mealId={}: {}", mealId, e.getMessage());
            meal.failAnalysis();
        }
    }

    private List<MealIngredient> buildMealIngredients(List<Map<String, Object>> rawIngredients) {
        List<MealIngredient> result = new ArrayList<>();

        for (Map<String, Object> raw : rawIngredients) {
            String name = (String) raw.get("name");
            Integer amountG = (Integer) raw.get("amount_g");
            Ingredient ingredient = findIngredient(name);

            if (ingredient == null) {
                // DB에 없는 식재료 → 이름만 기록, 수치 0
                result.add(MealIngredient.builder()
                        .name(name)
                        .amountG(amountG)
                        .co2(BigDecimal.ZERO)
                        .calories(BigDecimal.ZERO)
                        .protein(BigDecimal.ZERO)
                        .fat(BigDecimal.ZERO)
                        .carbs(BigDecimal.ZERO)
                        .fiber(BigDecimal.ZERO)
                        .calcium(BigDecimal.ZERO)
                        .iron(BigDecimal.ZERO)
                        .vitaminB12(BigDecimal.ZERO)
                        .vitaminD(BigDecimal.ZERO)
                        .omega3(BigDecimal.ZERO)
                        .build());
                continue;
            }

            // DB 수치는 100g 기준 → 실제 섭취량 비례 환산
            BigDecimal ratio = BigDecimal.valueOf(amountG)
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

            result.add(MealIngredient.builder()
                    .ingredientId(ingredient.getId())
                    .name(name)
                    .amountG(amountG)
                    .co2(nullSafe(ingredient.getCo2Per100g()).multiply(ratio))
                    .calories(nullSafe(ingredient.getCalories()).multiply(ratio))
                    .protein(nullSafe(ingredient.getProtein()).multiply(ratio))
                    .fat(nullSafe(ingredient.getFat()).multiply(ratio))
                    .carbs(nullSafe(ingredient.getCarbs()).multiply(ratio))
                    .fiber(nullSafe(ingredient.getFiber()).multiply(ratio))
                    .calcium(nullSafe(ingredient.getCalcium()).multiply(ratio))
                    .iron(nullSafe(ingredient.getIron()).multiply(ratio))
                    .vitaminB12(nullSafe(ingredient.getVitaminB12()).multiply(ratio))
                    .vitaminD(nullSafe(ingredient.getVitaminD()).multiply(ratio))
                    .omega3(nullSafe(ingredient.getOmega3()).multiply(ratio))
                    .build());
        }

        return result;
    }

    private Ingredient findIngredient(String name) {
        var exact = ingredientRepository.findFirstByName(name);
        if (exact.isPresent()) return exact.get();

        var partial = ingredientRepository.findByNameContaining(name);
        if (!partial.isEmpty()) return partial.get(0);

        return null;
    }

    private Map<String, BigDecimal> calculateNutrition(List<MealIngredient> ingredients) {
        Map<String, BigDecimal> nutrition = new HashMap<>();

        List.of("calories", "protein", "fat", "carbs", "fiber",
                        "calcium", "iron", "vitamin_b12", "vitamin_d", "omega3")
                .forEach(key -> nutrition.put(key, BigDecimal.ZERO));

        for (MealIngredient mi : ingredients) {
            nutrition.merge("calories",    nullSafe(mi.getCalories()),   BigDecimal::add);
            nutrition.merge("protein",     nullSafe(mi.getProtein()),    BigDecimal::add);
            nutrition.merge("fat",         nullSafe(mi.getFat()),        BigDecimal::add);
            nutrition.merge("carbs",       nullSafe(mi.getCarbs()),      BigDecimal::add);
            nutrition.merge("fiber",       nullSafe(mi.getFiber()),      BigDecimal::add);
            nutrition.merge("calcium",     nullSafe(mi.getCalcium()),    BigDecimal::add);
            nutrition.merge("iron",        nullSafe(mi.getIron()),       BigDecimal::add);
            nutrition.merge("vitamin_b12", nullSafe(mi.getVitaminB12()), BigDecimal::add);
            nutrition.merge("vitamin_d",   nullSafe(mi.getVitaminD()),   BigDecimal::add);
            nutrition.merge("omega3",      nullSafe(mi.getOmega3()),     BigDecimal::add);
        }

        return nutrition;
    }

    private void updateCarbonDaily(Meal meal, BigDecimal mealCarbon) {
        LocalDate today = LocalDate.now();

        CarbonDaily carbonDaily = carbonDailyRepository
                .findByUserIdAndCarbonDate(meal.getUser().getId(), today)
                .map(existing -> {
                    existing.addCarbon(mealCarbon);
                    return existing;
                })
                .orElseGet(() -> CarbonDaily.builder()
                        .user(meal.getUser())
                        .carbonDate(today)
                        .totalCarbon(mealCarbon)
                        .mealCount(1)
                        .build());

        carbonDailyRepository.save(carbonDaily);
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}