package com.veganing.domain.challenge.controller;

import com.veganing.domain.challenge.dto.ChallengeResponse;
import com.veganing.domain.challenge.dto.ChallengeStartRequest;
import com.veganing.domain.challenge.dto.ChallengeStatsResponse;
import com.veganing.domain.challenge.dto.PointAddRequest;
import com.veganing.domain.challenge.service.ChallengeService;
import com.veganing.global.auth.CustomUserDetails;
import com.veganing.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/challenge")
public class ChallengeController {

    private final ChallengeService challengeService;

    // 챌린지 시작
    @PostMapping("/start")
    public ResponseEntity<ApiResponse<ChallengeResponse>> startChallenge(
            @Valid @RequestBody ChallengeStartRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        String email = userDetails.getEmail();

        ChallengeResponse response = challengeService.startChallenge(request, email);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("챌린지 시작 성공", response));
    }

    // 진헹중인 챌린지 조회
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<ChallengeResponse>> getCurrentChallenge(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String email = userDetails.getEmail();

        ChallengeResponse response = challengeService.getCurrentChallenge(email);
        return ResponseEntity.ok(ApiResponse.success("진행중인 챌린지 조회 성공", response));
    }

    // 히스토리 조회
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<ChallengeResponse>>> getChallengeHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String email = userDetails.getEmail();

        List<ChallengeResponse> response = challengeService.getChallengeHistory(email);
        return ResponseEntity.ok(ApiResponse.success("히스토리 조회 성공", response));
    }

    // 통계 조회
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<ChallengeStatsResponse>> getChallengeStats(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String email = userDetails.getEmail();

        ChallengeStatsResponse response = challengeService.getChallengeStats(email);
        return ResponseEntity.ok(ApiResponse.success("통계 조회 성공", response));
    }

    // 챌린지 포기
    @PutMapping("/{id}/quit")
    public ResponseEntity<ApiResponse<Void>> quitChallenge(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String email = userDetails.getEmail();

        challengeService.quitChallenge(id, email);
        return ResponseEntity.ok(ApiResponse.success("챌린지 포기 성공", null));
    }

    // 포인트 추가
    @PostMapping("/add-points")
    public ResponseEntity<ApiResponse<Void>> addPoints(
            @Valid @RequestBody PointAddRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String email = userDetails.getEmail();
        challengeService.addPoints(request, email);
        return ResponseEntity.ok(ApiResponse.success("포인트 추가 성공", null));
    }
}
