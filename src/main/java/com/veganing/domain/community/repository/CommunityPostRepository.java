package com.veganing.domain.community.repository;

import com.veganing.domain.community.entity.CommunityPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

// 게시물 목록 조회 시 페이징이 필요. Spring Data JPA 는 Pageable 을 파라미터로 넣으면 자동으로 페이징 처리해줌

public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {

    Page<CommunityPost> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
