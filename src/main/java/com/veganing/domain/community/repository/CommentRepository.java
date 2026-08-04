package com.veganing.domain.community.repository;

import com.veganing.domain.community.entity.Comment;
import com.veganing.domain.community.entity.CommunityPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findAllByPostOrderByCreatedAtAsc(CommunityPost post);
}