package com.veganing.domain.meal.entity;

import com.veganing.domain.auth.entity.User;
import com.veganing.domain.challenge.entity.Challenge;
import com.veganing.domain.meal.dto.MealIngredient;
import com.veganing.domain.meal.enums.MealStatus;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "meals")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Meal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 식단을 올린 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 챌린지 진행 중에 올린 식단이면 연결, 아니면 null
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id")
    private Challenge challenge;

    // S3에 업로드된 식단 사진 URL
    @Column(name = "image_url", nullable = false, columnDefinition = "TEXT")
    private String imageUrl;

    /**
     * GPT-4 Vision이 추출한 식재료 목록 + 분석 시점 수치 스냅샷.
     * ingredients 테이블과 FK 없음 - 과거 기록 불변 보장.
     * 예: [{"ingredient_id": 12, "name": "두부", "amount_g": 150, "co2": 3.0, ...}]
     */
    @Type(JsonType.class)
    @Column(name = "ingredients", columnDefinition = "jsonb")
    @Builder.Default
    private List<MealIngredient> ingredients = new ArrayList<>();

    // ingredients 기준으로 합산한 총 탄소 절감량 (kg)
    @Column(name = "total_carbon", precision = 10, scale = 3)
    private BigDecimal totalCarbon;

    /**
     * 총 영양소 합계.
     * 예: {"calories": 450.0, "protein": 22.0, "vitamin_b12": 0.0, ...}
     */
    @Type(JsonType.class)
    @Column(name = "nutrition", columnDefinition = "jsonb")
    private Map<String, BigDecimal> nutrition;

    // GPT가 생성한 피드백 (부족 영양소 + 개선 제안)
    @Column(name = "ai_feedback", columnDefinition = "text")
    private String aiFeedback;

    // 분석 상태 - POST /api/meal 요청 시 ANALYZING으로 시작, 비동기 완료 후 DONE/FAILED
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MealStatus status = MealStatus.ANALYZING;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // 사용자의 비건 단계에 맞는 식단인지 여부 (AI 판단)
    private Boolean isVeganCompliant;

    // 비건 단계 위반 항목 목록 (예: ["달걀 포함", "치즈 포함"])
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private List<String> veganViolations;

    // ──────────────────────────────────────────
    // 비즈니스 메서드
    // ──────────────────────────────────────────

    /**
     * AI 분석 완료 후 결과 저장.
     * Service에서 직접 필드를 건드리지 않고 메서드를 통해 상태 변경.
     */
    public void completeAnalysis(List<MealIngredient> ingredients,
                                 BigDecimal totalCarbon,
                                 Map<String, BigDecimal> nutrition,
                                 String aiFeedback,
                                 Boolean isVeganCompliant,
                                 List<String> veganViolations) {
        this.ingredients = ingredients;
        this.totalCarbon = totalCarbon;
        this.nutrition = nutrition;
        this.aiFeedback = aiFeedback;
        this.isVeganCompliant = isVeganCompliant;
        this.veganViolations = veganViolations;
        this.status = MealStatus.DONE;
    }

    /**
     * AI 분석 실패 처리.
     */
    public void failAnalysis() {
        this.status = MealStatus.FAILED;
    }
}