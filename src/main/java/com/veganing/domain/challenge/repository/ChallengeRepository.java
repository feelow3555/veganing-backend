package com.veganing.domain.challenge.repository;

import com.veganing.domain.auth.entity.User;
import com.veganing.domain.challenge.entity.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChallengeRepository extends JpaRepository<Challenge, Long> {

    // 진행중인 챌린지 조회
    Optional<Challenge> findByUserAndStatus(User user, String status);

    // 히스토리 조회 (최신순)
    List<Challenge> findByUserOrderByCreatedAtDesc(User user);

    // 통계용 - 전체 참여 수
    int countByUser(User user);

    // 통계용 - 완료 수
    int countByUserAndStatus(User user, String status);
}
