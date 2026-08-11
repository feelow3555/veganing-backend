package com.veganing.domain.community.repository;

import com.veganing.domain.community.entity.CommunityPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

// 게시물 목록 조회 시 페이징이 필요. Spring Data JPA 는 Pageable 을 파라미터로 넣으면 자동으로 페이징 처리해줌

public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {

    Page<CommunityPost> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // 좋아요 상위 N개 게시물 조회 (스케줄러용)
    // Pageable로 개수 제한
    @Query("SELECT p FROM CommunityPost p ORDER BY p.likeCount DESC")
    List<CommunityPost> findTopByLikeCount(Pageable pageable);
}
