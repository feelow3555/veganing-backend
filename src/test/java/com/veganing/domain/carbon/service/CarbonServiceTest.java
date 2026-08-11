package com.veganing.domain.carbon.service;

import com.veganing.domain.carbon.dto.CarbonHistoryResponse;
import com.veganing.domain.carbon.dto.CarbonStatsResponse;
import com.veganing.domain.carbon.dto.CarbonTodayResponse;
import com.veganing.domain.carbon.entity.CarbonDaily;
import com.veganing.domain.carbon.repository.CarbonDailyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class CarbonServiceTest {

    @InjectMocks
    private CarbonService carbonService;

    @Mock
    private CarbonDailyRepository carbonDailyRepository;

    private static final Long USER_ID = 1L;
    private static final BigDecimal KOREAN_DAILY_AVG = new BigDecimal("5.5");

    private CarbonDaily mockCarbonDaily;

    @BeforeEach
    void setUp() {
        mockCarbonDaily = CarbonDaily.builder()
                .carbonDate(LocalDate.now())
                .totalCarbon(new BigDecimal("2.5"))
                .mealCount(2)
                .build();
    }

    @Test
    @DisplayName("전체 누적 통계 조회 시 총 절감량, 식단 수, 일평균을 반환한다")
    void getStats_success() {
        // given
        given(carbonDailyRepository.sumTotalCarbonByUserId(USER_ID)).willReturn(new BigDecimal("10.0"));
        given(carbonDailyRepository.sumMealCountByUserId(USER_ID)).willReturn(5);
        given(carbonDailyRepository.countByUserId(USER_ID)).willReturn(3L);

        // when
        CarbonStatsResponse result = carbonService.getStats(USER_ID);

        // then
        assertThat(result.getTotalCarbon()).isEqualByComparingTo("10.0");
        assertThat(result.getTotalMealCount()).isEqualTo(5);
        // 일평균 = 10.0 / 3 = 3.33
        assertThat(result.getAvgDailyCarbon()).isEqualByComparingTo("3.33");
    }

    @Test
    @DisplayName("활동일수가 0이면 일평균 절감량은 0을 반환한다")
    void getStats_noActivity() {
        // given
        given(carbonDailyRepository.sumTotalCarbonByUserId(USER_ID)).willReturn(BigDecimal.ZERO);
        given(carbonDailyRepository.sumMealCountByUserId(USER_ID)).willReturn(0);
        given(carbonDailyRepository.countByUserId(USER_ID)).willReturn(0L);

        // when
        CarbonStatsResponse result = carbonService.getStats(USER_ID);

        // then
        assertThat(result.getAvgDailyCarbon()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("오늘 식단 데이터가 있으면 절감량과 식단 수를 반환한다")
    void getToday_success() {
        // given
        given(carbonDailyRepository.findByUserIdAndCarbonDate(USER_ID, LocalDate.now()))
                .willReturn(Optional.of(mockCarbonDaily));

        // when
        CarbonTodayResponse result = carbonService.getToday(USER_ID);

        // then
        assertThat(result.getTotalCarbon()).isEqualByComparingTo("2.5");
        assertThat(result.getMealCount()).isEqualTo(2);
        // savedCarbon = 5.5 - 2.5 = 3.0
        assertThat(result.getSavedCarbon()).isEqualByComparingTo("3.0");
    }

    @Test
    @DisplayName("오늘 식단 데이터가 없으면 절감량 0, savedCarbon은 한국인 평균(5.5)을 반환한다")
    void getToday_noData() {
        // given
        given(carbonDailyRepository.findByUserIdAndCarbonDate(USER_ID, LocalDate.now()))
                .willReturn(Optional.empty());

        // when
        CarbonTodayResponse result = carbonService.getToday(USER_ID);

        // then
        assertThat(result.getTotalCarbon()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getMealCount()).isEqualTo(0);
        assertThat(result.getSavedCarbon()).isEqualByComparingTo(KOREAN_DAILY_AVG);
    }

    @Test
    @DisplayName("기간별 일별 절감량 목록을 날짜 오름차순으로 반환한다")
    void getHistory_success() {
        // given
        List<CarbonDaily> mockList = List.of(
                CarbonDaily.builder()
                        .carbonDate(LocalDate.now().minusDays(1))
                        .totalCarbon(new BigDecimal("1.5"))
                        .mealCount(1)
                        .build(),
                CarbonDaily.builder()
                        .carbonDate(LocalDate.now())
                        .totalCarbon(new BigDecimal("2.5"))
                        .mealCount(2)
                        .build()
        );
        given(carbonDailyRepository.findByUserIdAndCarbonDateBetweenOrderByCarbonDateAsc(
                eq(USER_ID), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(mockList);

        // when
        List<CarbonHistoryResponse> result = carbonService.getHistory(USER_ID, 7);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTotalCarbon()).isEqualByComparingTo("1.5");
        assertThat(result.get(1).getTotalCarbon()).isEqualByComparingTo("2.5");
    }

    @Test
    @DisplayName("기간 내 데이터가 없으면 빈 리스트를 반환한다")
    void getHistory_empty() {
        // given
        given(carbonDailyRepository.findByUserIdAndCarbonDateBetweenOrderByCarbonDateAsc(
                eq(USER_ID), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(List.of());

        // when
        List<CarbonHistoryResponse> result = carbonService.getHistory(USER_ID, 7);

        // then
        assertThat(result).isEmpty();
    }
}