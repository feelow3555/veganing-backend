package com.veganing.domain.ingredient.repository;

import com.veganing.domain.ingredient.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {

    // 완전 일치 - "두부" → name = '두부'
    // First 붙이는 이유: 같은 이름 row 여러 개일 때 예외 방지
    Optional<Ingredient> findFirstByName(String name);

    // 부분 일치 - "국산 두부" → name LIKE '%두부%'
    // 완전 일치 실패 시 폴백, 여러 개 나올 수 있으니 List
    List<Ingredient> findByNameContaining(String name);

    // 카테고리 평균 - 이름으로 아예 못 찾을 때 마지막 폴백
    // ex) 알 수 없는 채소 → VEGETABLE 평균값으로 대체
    @Query("""
            SELECT AVG(i.co2Per100g), AVG(i.calories), AVG(i.protein),
                   AVG(i.fat), AVG(i.carbs), AVG(i.fiber),
                   AVG(i.calcium), AVG(i.iron),
                   AVG(i.vitaminB12), AVG(i.vitaminD), AVG(i.omega3)
            FROM Ingredient i
            WHERE i.category = :category
            """)
    List<Object[]> findAvgByCategory(@Param("category") String category);
}