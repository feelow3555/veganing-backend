package com.veganing.domain.challenge.repository;

import com.veganing.domain.auth.entity.User;
import com.veganing.domain.challenge.entity.PointHistory;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {

    // 누적 포인트 합계
    @Query("SELECT COALESCE(SUM(p.points), 0) FROM PointHistory p WHERE p.user = :user")
    Integer sumPointsByUser(@Param("user") User user);

    // 기간별 포인트 합계 (랭킹용) — region nullable
    @Query("""
    SELECT p.user.id, p.user.nickname, p.user.region, COALESCE(SUM(p.points), 0)
    FROM PointHistory p
    WHERE (:region IS NULL OR p.user.region = :region)
      AND p.createdAt >= :from
    GROUP BY p.user.id, p.user.nickname, p.user.region
    ORDER BY SUM(p.points) DESC
    LIMIT 10
    """)
    List<Object[]> findRankingByPeriod(
            @Param("region") String region,
            @Param("from") LocalDateTime from
    );

    // 전체 누적 랭킹 (all)
    @Query("""
    SELECT p.user.id, p.user.nickname, p.user.region, COALESCE(SUM(p.points), 0)
    FROM PointHistory p
    WHERE (:region IS NULL OR p.user.region = :region)
    GROUP BY p.user.id, p.user.nickname, p.user.region
    ORDER BY SUM(p.points) DESC
    LIMIT 10
    """)
    List<Object[]> findRankingAll(@Param("region") String region);
}
