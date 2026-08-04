package com.veganing.domain.community.controller;

import com.veganing.domain.community.dto.*;
import com.veganing.domain.community.service.CommunityService;
import com.veganing.global.auth.CustomUserDetails;
import com.veganing.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/community")
public class CommunityController {

    private final CommunityService communityService;

    // 게시물 작성
    @PostMapping("/posts")
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @Valid @RequestBody PostCreateRequest request, // 클라이언트가 보낸 JSON 을 Java 객체로 변환
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String email = userDetails.getEmail();

        PostResponse response = communityService.createPost(request, email);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("게시물 작성 성공", response));
    }

    // 게시물 목록 조회
    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<Page<PostResponse>>> getPosts(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<PostResponse> posts = communityService.getPosts(pageable);

        return ResponseEntity.ok(ApiResponse.success("게시물 조회 성공", posts));
    }

    // 게시물 상세 조회
    @GetMapping("/posts/{id}")
    public ResponseEntity<ApiResponse<PostResponse>> getPost(
            @PathVariable Long id) { // @PathVariable -> URL 경로에서 값을 꺼냄
        PostResponse post = communityService.getPost(id);

        return ResponseEntity.ok(ApiResponse.success("게시물 조회 성공", post));
    }

    // 게시물 수정
    @PutMapping("/posts/{id}")
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(
            @PathVariable Long id,
            @Valid @RequestBody PostUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String email = userDetails.getEmail();

        PostResponse response = communityService.updatePost(id, request, email);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("게시물 수정 성공", response));
    }

    // 게시물 삭제
    @DeleteMapping("/posts/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String email = userDetails.getEmail();

        communityService.deletePost(id, email);
        return ResponseEntity.ok(ApiResponse.success("게시물 삭제 성공", null));
    }

    // 좋아요 토글
    @PostMapping("/posts/{id}/like")
    public ResponseEntity<ApiResponse<Void>> toggleLike(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String email = userDetails.getEmail();

        communityService.toggleLike(id, email);
        return ResponseEntity.ok(ApiResponse.success("좋아요 성공", null));
    }

    // 댓글 작성
    @PostMapping("/posts/{id}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @PathVariable Long id,
            @Valid @RequestBody CommentCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String email = userDetails.getEmail();

        CommentResponse response = communityService.createComment(id, request, email);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("댓글 작성 성공", response));
    }

    // 댓글 목록 조회
    @GetMapping("/posts/{id}/comments")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getComments(
            @PathVariable Long id) {
        List<CommentResponse> comments = communityService.getComments(id);
        return ResponseEntity.ok(ApiResponse.success("댓글 목록 조회 성공", comments));
    }

    // 댓글 삭제
    @DeleteMapping("/comments/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        String email = userDetails.getEmail();

        communityService.deleteComment(id, email);
        return ResponseEntity.ok(ApiResponse.success("댓글 삭제 성공", null));
    }
}

/*
    @AuthenticationPrincipal 흐름

    1. 클라이언트가 요청할 때 헤더에 토큰을 담아서 보냄
    Authorization: Bearer eyJhbGci...

    2. JwtFilter 가 토큰을 꺼내서 검증
    → 토큰에서 email 추출
    → SecurityContextHolder 에 저장

    3. Controller 에서 @AuthenticationPrincipal 로 꺼냄
    → userDetails.getEmail(), userDetails.getUserId() 로 꺼냄
*/
