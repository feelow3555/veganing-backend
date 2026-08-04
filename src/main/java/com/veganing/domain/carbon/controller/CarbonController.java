package com.veganing.domain.carbon.controller;

import com.veganing.domain.carbon.dto.CarbonHistoryResponse;
import com.veganing.domain.carbon.dto.CarbonStatsResponse;
import com.veganing.domain.carbon.dto.CarbonTodayResponse;
import com.veganing.domain.carbon.service.CarbonService;
import com.veganing.global.auth.CustomUserDetails;
import com.veganing.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/carbon")
public class CarbonController {

    private final CarbonService carbonService;

    // 전체 누적 통계 조회
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<CarbonStatsResponse>> getStats(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        CarbonStatsResponse response = carbonService.getStats(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("탄소 절감 통계 조회 성공", response));
    }

    // 오늘 절감량 조회
    @GetMapping("/today")
    public ResponseEntity<ApiResponse<CarbonTodayResponse>> getToday(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        CarbonTodayResponse response = carbonService.getToday(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("오늘 절감량 조회 성공", response));
    }

    // 기간별 일별 집계 조회
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<CarbonHistoryResponse>>> getHistory(
            @RequestParam(defaultValue = "7") int days,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<CarbonHistoryResponse> response = carbonService.getHistory(userDetails.getUserId(), days);
        return ResponseEntity.ok(ApiResponse.success("기간별/일별 집계 조회 성공", response));
    }
}
