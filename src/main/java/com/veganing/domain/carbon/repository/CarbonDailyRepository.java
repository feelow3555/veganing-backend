package com.veganing.domain.carbon.repository;

import com.veganing.domain.carbon.entity.CarbonDaily;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CarbonDailyRepository extends JpaRepository<CarbonDaily, Long> {

    // 1. 오늘 데이터 조회
    Optional<CarbonDaily> findByUserIdAndCarbonDate(Long userId, LocalDate date);

    // 2. 기간별 데이터 조회 (history)
    List<CarbonDaily> findByUserIdAndCarbonDateBetweenOrderByCarbonDateAsc(
            Long userId, LocalDate start, LocalDate end);

    // 3. 활동일수
    long countByUserId(Long userId);

    // 4. 전체 누적 절감량 합계
    @Query("SELECT COALESCE(SUM(c.totalCarbon), 0) FROM CarbonDaily c WHERE c.user.id = :userId")
    BigDecimal sumTotalCarbonByUserId(@Param("userId") Long userId);

    // 5. 전체 식단 수 합계
    @Query("SELECT COALESCE(SUM(c.mealCount), 0) FROM CarbonDaily c WHERE c.user.id = :userId")
    int sumMealCountByUserId(@Param("userId") Long userId);
}
