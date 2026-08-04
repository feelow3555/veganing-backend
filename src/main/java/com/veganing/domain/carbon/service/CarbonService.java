package com.veganing.domain.carbon.service;

import com.veganing.domain.carbon.dto.CarbonHistoryResponse;
import com.veganing.domain.carbon.dto.CarbonStatsResponse;
import com.veganing.domain.carbon.dto.CarbonTodayResponse;
import com.veganing.domain.carbon.entity.CarbonDaily;
import com.veganing.domain.carbon.repository.CarbonDailyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CarbonService {

    private final CarbonDailyRepository carbonDailyRepository;

    // 전체 누적 통계 조회 (총 절감량, 총 식단 수, 일평균 절감량) 메서드
    public CarbonStatsResponse getStats(Long userId) {
        // 1. 전체 누적 절감량 합계 조회 (sumTotalCarbonByUserId)
        BigDecimal totalCarbon = carbonDailyRepository.sumTotalCarbonByUserId(userId);

        // 2. 전체 식단 수 합계 조회 (sumMealCountByUserId)
        int totalMealCount = carbonDailyRepository.sumMealCountByUserId(userId);

        // 3. 활동일수 조회
        long activeDays = carbonDailyRepository.countByUserId(userId);

        // 일평균 계산 (활동일수 0이면 0 반환)
        BigDecimal avgDailyCarbon = activeDays == 0
                ? BigDecimal.ZERO
                : totalCarbon.divide(new BigDecimal(activeDays), 2, RoundingMode.HALF_UP);

        // 4. CarbonStatsResponse 빌드 후 반환
        return CarbonStatsResponse.builder()
                .totalCarbon(totalCarbon)
                .totalMealCount(totalMealCount)
                .avgDailyCarbon(avgDailyCarbon)
                .build();
    }

    // 오늘 절감량 + 식단 수 조회 (오늘 데이터 없으면 0으로 반환) 메서드
    public CarbonTodayResponse getToday(Long userId) {
        // 1. 오늘 날짜로 carbon_daily 조회
        Optional<CarbonDaily> carbonDaily = carbonDailyRepository.findByUserIdAndCarbonDate(userId, LocalDate.now());

        // 2. 데이터 없으면 0, 있으면 DTO 빌드 후 반환
        return carbonDaily
                .map(c -> CarbonTodayResponse.builder()
                        .totalCarbon(c.getTotalCarbon())
                        .mealCount(c.getMealCount())
                        .build())
                .orElse(CarbonTodayResponse.builder()
                        .totalCarbon(BigDecimal.ZERO)
                        .mealCount(0)
                        .build());
    }

    // 최근 N일 일별 절감량 목록 조회 (Recharts 용)
    public List<CarbonHistoryResponse> getHistory(Long userId, int days) {
        // 1. 오늘 기준 days 일 전 날짜 계산
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days);

        // 2. 기간별 carbon_daily 목록 조회
        List<CarbonDaily> carbonDailyList = carbonDailyRepository
                .findByUserIdAndCarbonDateBetweenOrderByCarbonDateAsc(userId, start, end);

        // 3. List<CarbonDaily> → List<CarbonHistoryResponse> 변환
        return carbonDailyList.stream()
                .map(c -> CarbonHistoryResponse.builder()
                        .date(c.getCarbonDate())
                        .totalCarbon(c.getTotalCarbon())
                        .mealCount(c.getMealCount())
                        .build())
                .collect(Collectors.toList());
    }
}
