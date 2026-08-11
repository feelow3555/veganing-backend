package com.veganing.domain.meal.repository;

import com.veganing.domain.auth.entity.User;
import com.veganing.domain.meal.entity.Meal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MealRepository extends JpaRepository<Meal, Long> {

    // 내 식단 기록 최신순 조회
    List<Meal> findByUserOrderByCreatedAtDesc(User user);
}