package com.veganing.domain.community.service;

import com.veganing.domain.auth.entity.User;
import com.veganing.domain.auth.repository.UserRepository;
import com.veganing.domain.community.dto.*;
import com.veganing.domain.community.entity.Comment;
import com.veganing.domain.community.entity.CommunityPost;
import com.veganing.domain.community.entity.PostLike;
import com.veganing.domain.community.repository.CommentRepository;
import com.veganing.domain.community.repository.CommunityPostRepository;
import com.veganing.domain.community.repository.PostLikeRepository;
import com.veganing.global.error.CustomException;
import com.veganing.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommunityService {

    private final UserRepository userRepository;
    private final CommunityPostRepository communityPostRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentRepository commentRepository;

    // 게시물 작성 메서드
    @Transactional
    public PostResponse createPost(PostCreateRequest request, String email) {
        // 1. userId로 User 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. Request + User 로 CommunityPost Entity 생성
        CommunityPost post = CommunityPost.builder()
                .user(user)
                .title(request.getTitle())
                .content(request.getContent())
                .imageUrl(request.getImageUrl())
                .ingredients(request.getIngredients())
                .steps(request.getSteps())
                .build();

        // 3. DB 저장
        CommunityPost savePost = communityPostRepository.save(post);

        //4. PostResponse 변환
        return PostResponse.builder()
                .id(savePost.getId())
                .title(savePost.getTitle())
                .content(savePost.getContent())
                .imageUrl(savePost.getImageUrl())
                .ingredients(savePost.getIngredients())
                .steps(savePost.getSteps())
                .nickname(savePost.getUser().getNickname())
                .likeCount(savePost.getLikeCount())
                .commentCount(post.getCommentCount())
                .createdAt(post.getCreatedAt())
                .build();
    }

    // 게시물 목록 조회 메서드
    public Page<PostResponse> getPosts(Pageable pageable) {
        // 1. communityPostRepository.findAllByOrderByCreatedAtDesc(pageable) 로 조회
        Page<CommunityPost> posts = communityPostRepository.findAllByOrderByCreatedAtDesc(pageable);

        // Page<CommunityPost> → Page<PostResponse> 로 변환해서 반환
        return posts.map(post -> PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .imageUrl(post.getImageUrl())
                .ingredients(post.getIngredients())
                .steps(post.getSteps())
                .nickname(post.getUser().getNickname())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .createdAt(post.getCreatedAt())
                .build()
        );
    }

    // 게시물 상세 조회 메서드
    public PostResponse getPost(Long postId) {
        // 1. postId로 CommunityPost 조회 (없으면 예외)
        CommunityPost communityPost = communityPostRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        // 2. PostResponse 로 변환해서 반환
        return PostResponse.builder()
                .id(communityPost.getId())
                .title(communityPost.getTitle())
                .content(communityPost.getContent())
                .imageUrl(communityPost.getImageUrl())
                .ingredients(communityPost.getIngredients())
                .steps(communityPost.getSteps())
                .nickname(communityPost.getUser().getNickname())
                .likeCount(communityPost.getLikeCount())
                .commentCount(communityPost.getCommentCount())
                .createdAt(communityPost.getCreatedAt())
                .build();
    }

    // 게시물 수정 메서드
    @Transactional
    public  PostResponse updatePost(Long postId, PostUpdateRequest request, String email) {
        // 1. postId로 CommunityPost 조회 (없으면 예외)
        CommunityPost communityPost = communityPostRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        // 2. 본인 글인지 확인 (userId 비교, 아니면 예외)
        if(!communityPost.getUser().getEmail().equals(email)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        // 3. 수정
        communityPost.update(request.getTitle(), request.getContent(), request.getImageUrl(), request.getIngredients(), request.getSteps());

        // 4. PostResponse 로 변환해서 반환
        return PostResponse.builder()
                .id(communityPost.getId())
                .title(communityPost.getTitle())
                .content(communityPost.getContent())
                .imageUrl(communityPost.getImageUrl())
                .ingredients(communityPost.getIngredients())
                .steps(communityPost.getSteps())
                .nickname(communityPost.getUser().getNickname())
                .likeCount(communityPost.getLikeCount())
                .commentCount(communityPost.getCommentCount())
                .createdAt(communityPost.getCreatedAt())
                .build();
    }

    // 게시물 삭제 메서드
    @Transactional
    public void deletePost(Long postId, String email) {
        // 1. postId로 CommunityPost 조회 (없으면 예외)
        CommunityPost communityPost = communityPostRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        // 2. 본인 글인지 확인 (userId 비교, 아니면 예외)
        if(!communityPost.getUser().getEmail().equals(email)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        // 3. 삭제
        communityPostRepository.delete(communityPost);
    }

    // 좋아요 토글 메서드
    @Transactional
    public void toggleLike(Long postId, String email) {
        // 1. postId로 CommunityPost 조회 (없으면 예외)
        CommunityPost communityPost = communityPostRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        // 2. userId로 User 조회 (없으면 예외)
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        /*
        3. 이미 좋아요 눌렀는지 확인
        - 눌렀으면 → PostLike 삭제 + likeCount -1
        - 안 눌렀으면 → PostLike 저장 + likeCount +1
        */
        boolean alreadyLiked = postLikeRepository.existsByPostAndUser(communityPost, user);

        if(alreadyLiked) {
            postLikeRepository.deleteByPostAndUser(communityPost, user);
            communityPost.decrementLikeCount();
        } else {
            postLikeRepository.save(PostLike.builder()
                            .post(communityPost)
                            .user(user)
                            .build());
            communityPost.incrementLikeCount();
        }
    }

    // 댓글 작성 메서드
    @Transactional
    public CommentResponse createComment(Long postId, CommentCreateRequest request, String email) {
        // 1. postId로 CommunityPost 조회 (없으면 예외)
        CommunityPost communityPost = communityPostRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        // 2. userId로 User 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 3. Comment Entity 생성
        Comment comment = Comment.builder()
                .post(communityPost)
                .user(user)
                .content(request.getContent())
                .build();

        // 4. DB 저장
        Comment saveComment = commentRepository.save(comment);
        communityPost.incrementCommentCount();

        // 5. CommentResponse 로 변환해서 반환
        return CommentResponse.builder()
                .id(saveComment.getId())
                .nickname(saveComment.getUser().getNickname())
                .content(saveComment.getContent())
                .createdAt(comment.getCreatedAt())
                .build();
    }

    // 댓글 목록 조회 메서드
    public List<CommentResponse> getComments(Long postId) {
        // 1. postId로 CommunityPost 조회 (없으면 예외)
        CommunityPost communityPost = communityPostRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        // 2. commentRepository.findAllByPostOrderByCreatedAtAsc(communityPost) 로 조회
        List<Comment> comments = commentRepository.findAllByPostOrderByCreatedAtAsc(communityPost);

        // 3. 3. List<Comment> → List<CommentResponse> 로 변환해서 반환
        return comments.stream()
                .map(comment -> CommentResponse.builder()
                        .id(comment.getId())
                        .nickname(comment.getUser().getNickname())
                        .content(comment.getContent())
                        .createdAt(comment.getCreatedAt())
                        .build()
                )
                .collect(Collectors.toList());
    }

    // 댓글 삭제 메서드
    @Transactional
    public void deleteComment(Long commentId, String email) {
        // 1. commentId로 Comment 조회 (없으면 예외)
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        // 2. 본인 글인지 확인 (userId 비교, 아니면 예외)
        if(!comment.getUser().getEmail().equals(email)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        // 3. 삭제
        commentRepository.delete(comment);
        comment.getPost().decrementCommentCount();
    }
}
