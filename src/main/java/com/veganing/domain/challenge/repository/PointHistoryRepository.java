package com.veganing.domain.challenge.repository;

import com.veganing.domain.auth.entity.User;
import com.veganing.domain.challenge.entity.PointHistory;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {

    // 누적 포인트 합계
    @Query("SELECT COALESCE(SUM(p.points), 0) FROM PointHistory p WHERE p.user = :user")
    Integer sumPointsByUser(@Param("user") User user);
}
