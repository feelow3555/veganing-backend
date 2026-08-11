package com.veganing.domain.meal.repository;

import com.veganing.domain.auth.entity.User;
import com.veganing.domain.meal.entity.Meal;
import com.veganing.domain.meal.enums.MealStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MealRepository extends JpaRepository<Meal, Long> {

    // 내 식단 기록 최신순 조회
    List<Meal> findByUserOrderByCreatedAtDesc(User user);

    // 가장 최근 완료된 식단 조회 (RAG 추천용)
    // DONE 상태인 것만 → 분석 완료된 식단만 추천에 활용
    Optional<Meal> findFirstByUserAndStatusOrderByCreatedAtDesc(User user, MealStatus status);
}