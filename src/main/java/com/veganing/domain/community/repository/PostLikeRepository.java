package com.veganing.domain.community.repository;

import com.veganing.domain.auth.entity.User;
import com.veganing.domain.community.entity.CommunityPost;
import com.veganing.domain.community.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

// 이미 좋아요를 눌렀는지(중복 방지), 좋아요 취소 시 삭제

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    boolean existsByPostAndUser(CommunityPost post, User user);

    void deleteByPostAndUser(CommunityPost post, User user);
}
