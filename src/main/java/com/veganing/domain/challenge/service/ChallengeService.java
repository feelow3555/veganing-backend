package com.veganing.domain.challenge.service;

import com.veganing.domain.auth.entity.User;
import com.veganing.domain.auth.repository.UserRepository;
import com.veganing.domain.challenge.dto.ChallengeResponse;
import com.veganing.domain.challenge.dto.ChallengeStartRequest;
import com.veganing.domain.challenge.dto.ChallengeStatsResponse;
import com.veganing.domain.challenge.dto.PointAddRequest;
import com.veganing.domain.challenge.entity.Challenge;
import com.veganing.domain.challenge.entity.PointHistory;
import com.veganing.domain.challenge.repository.ChallengeRepository;
import com.veganing.domain.challenge.repository.PointHistoryRepository;
import com.veganing.global.error.CustomException;
import com.veganing.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChallengeService {

    private final ChallengeRepository challengeRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final UserRepository userRepository;

    // 챌린지 시작 메서드
    @Transactional
    public ChallengeResponse startChallenge(ChallengeStartRequest request, String email) {

        // 1. 유저 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 이미 진행중인 챌린지 있는지 확인 → 있으면 예외
        if(challengeRepository.findByUserAndStatus(user, "ONGOING").isPresent()) {
            throw new CustomException(ErrorCode.CHALLENGE_ALREADY_EXISTS);
        }

        // 3. Challenge 생성 + 저장
        Challenge challenge = Challenge.builder()
                .user(user)
                .type(request.getType())
                .durationDays(request.getDuration())
                .purpose(request.getPurpose())
                .status("ONGOING")
                .currentDay(1)
                .startedAt(LocalDateTime.now())
                .build();

        challengeRepository.save(challenge);

        // 4. ChallengeResponse 반환
        return ChallengeResponse.builder()
                .id(challenge.getId())
                .type(request.getType())
                .durationDays(request.getDuration())
                .status(challenge.getStatus())
                .currentDay(challenge.getCurrentDay())
                .startedAt(challenge.getStartedAt())
                .endedAt(challenge.getEndedAt())
                .build();
    }

    // 진행중인 챌린지 조회
    @Transactional(readOnly = true)
    public ChallengeResponse getCurrentChallenge(String email) {
        // 1. 유저 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. ONGOING 챌린지 조회 → 없으면 예외
        Challenge challenge = challengeRepository.findByUserAndStatus(user, "ONGOING")
                .orElseThrow(() -> new CustomException(ErrorCode.CHALLENGE_NOT_FOUND));

        // 3. ChallengeResponse 반환
        return ChallengeResponse.builder()
                .id(challenge.getId())
                .type(challenge.getType())
                .durationDays(challenge.getDurationDays())
                .status(challenge.getStatus())
                .currentDay(challenge.getCurrentDay())
                .startedAt(challenge.getStartedAt())
                .endedAt(challenge.getEndedAt())
                .build();
    }

    // 히스토리 조회
    @Transactional(readOnly = true)
    public List<ChallengeResponse> getChallengeHistory(String email) {
        // 1. 유저 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 전체 챌린지 목록 조회 (최신순)
        List<Challenge> challenges = challengeRepository.findByUserOrderByCreatedAtDesc(user);

        // 3. List<ChallengeResponse> 반환
        return challenges.stream()
                .map(challenge -> ChallengeResponse.builder()
                        .id(challenge.getId())
                        .type(challenge.getType())
                        .durationDays(challenge.getDurationDays())
                        .status(challenge.getStatus())
                        .currentDay(challenge.getCurrentDay())
                        .startedAt(challenge.getStartedAt())
                        .endedAt(challenge.getEndedAt())
                        .build()
                )
                .collect(Collectors.toList());
    }

    // 통계 조회
    @Transactional(readOnly = true)
    public ChallengeStatsResponse getChallengeStats(String email) {
        // 1. 유저 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 전체 참여 수 조회
        int totalChallenges = challengeRepository.countByUser(user);

        // 3. 완료 수 조회
        int completedChallenges = challengeRepository.countByUserAndStatus(user, "COMPLETED");

        // 4. 누적 포인트 조회 (PointHistoryRepository)

        int totalPoints = pointHistoryRepository.sumPointsByUser(user);

        // 5. ChallengeStatsResponse 반환
        return ChallengeStatsResponse.builder()
                .totalChallenges(totalChallenges)
                .completedChallenges(completedChallenges)
                .totalPoints(totalPoints)
                .currentStreak(0) // 나중 메서드 구현
                .build();
    }

    // 챌린지 포기
    @Transactional
    public void quitChallenge(Long challengeId, String email) {
        // 1. 유저 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 챌린지 조회 → 없으면 예외
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHALLENGE_NOT_FOUND));

        // 3. 본인 챌린지인지 확인 → 아니면 예외
        if(!challenge.getUser().getEmail().equals(email)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        // 4. status → "QUIT", endedAt → 현재시각 업데이트
        challenge.quit();
    }

    // 포인트 추가
    @Transactional
    public void addPoints(PointAddRequest request, String email) {
        // 1. 유저 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. PointHistory 생성 + 저장
        PointHistory pointHistory = PointHistory.builder()
                .user(user)
                .points(request.getPoints())
                .reason(request.getReason())
                .build();

        pointHistoryRepository.save(pointHistory);

        // 3. user.totalPoints 업데이트
        user.addPoints(request.getPoints());
    }
}
