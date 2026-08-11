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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class CommunityServiceTest {

    @InjectMocks
    private CommunityService communityService;

    @Mock private UserRepository userRepository;
    @Mock private CommunityPostRepository communityPostRepository;
    @Mock private PostLikeRepository postLikeRepository;
    @Mock private CommentRepository commentRepository;

    private User mockUser;
    private CommunityPost mockPost;
    private Comment mockComment;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .email("test@test.com")
                .nickname("테스터")
                .build();

        mockPost = CommunityPost.builder()
                .user(mockUser)
                .title("비건 두부 샐러드")
                .content("맛있는 레시피입니다")
                .build();

        mockComment = Comment.builder()
                .post(mockPost)
                .user(mockUser)
                .content("맛있겠다!")
                .build();
    }

    @Test
    @DisplayName("게시물 작성 시 PostResponse를 반환한다")
    void createPost_success() throws Exception {
        // given
        PostCreateRequest request = new PostCreateRequest();
        setField(request, "title", "비건 두부 샐러드");
        setField(request, "content", "맛있는 레시피입니다");
        setField(request, "imageUrl", "https://s3.amazonaws.com/test.jpg");
        setField(request, "ingredients", List.of(Map.of("name", "두부")));
        setField(request, "steps", List.of(Map.of("step", "1. 두부를 썬다")));

        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(mockUser));
        given(communityPostRepository.save(any(CommunityPost.class))).willReturn(mockPost);

        // when
        PostResponse result = communityService.createPost(request, "test@test.com");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("비건 두부 샐러드");
        then(communityPostRepository).should().save(any(CommunityPost.class));
    }

    @Test
    @DisplayName("존재하지 않는 유저로 게시물 작성 시 USER_NOT_FOUND 예외가 발생한다")
    void createPost_userNotFound() throws Exception {
        // given
        PostCreateRequest request = new PostCreateRequest();
        setField(request, "title", "제목");
        setField(request, "content", "내용");
        setField(request, "imageUrl", "https://s3.amazonaws.com/test.jpg");
        setField(request, "ingredients", List.of(Map.of("name", "두부")));
        setField(request, "steps", List.of(Map.of("step", "1. 두부를 썬다")));

        given(userRepository.findByEmail(any())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> communityService.createPost(request, "none@test.com"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("게시물 목록 조회 시 Page<PostResponse>를 반환한다")
    void getPosts_success() {
        // given
        Page<CommunityPost> mockPage = new PageImpl<>(List.of(mockPost));
        given(communityPostRepository.findAllByOrderByCreatedAtDesc(any())).willReturn(mockPage);

        // when
        Page<PostResponse> result = communityService.getPosts(PageRequest.of(0, 10));

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("비건 두부 샐러드");
    }

    @Test
    @DisplayName("게시물 상세 조회 시 PostResponse를 반환한다")
    void getPost_success() {
        // given
        given(communityPostRepository.findById(1L)).willReturn(Optional.of(mockPost));

        // when
        PostResponse result = communityService.getPost(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("비건 두부 샐러드");
    }

    @Test
    @DisplayName("존재하지 않는 게시물 조회 시 POST_NOT_FOUND 예외가 발생한다")
    void getPost_notFound() {
        // given
        given(communityPostRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> communityService.getPost(999L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_NOT_FOUND);
    }

    @Test
    @DisplayName("본인 게시물 수정 시 수정된 PostResponse를 반환한다")
    void updatePost_success() throws Exception {
        // given
        PostUpdateRequest request = new PostUpdateRequest();
        setField(request, "title", "수정된 제목");
        setField(request, "content", "수정된 내용");
        setField(request, "imageUrl", "https://s3.amazonaws.com/test.jpg");
        setField(request, "ingredients", List.of(Map.of("name", "두부")));
        setField(request, "steps", List.of(Map.of("step", "1. 두부를 썬다")));

        given(communityPostRepository.findById(1L)).willReturn(Optional.of(mockPost));

        // when
        PostResponse result = communityService.updatePost(1L, request, "test@test.com");

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("타인 게시물 수정 시 UNAUTHORIZED 예외가 발생한다")
    void updatePost_unauthorized() throws Exception {
        // given
        PostUpdateRequest request = new PostUpdateRequest();
        setField(request, "title", "수정된 제목");
        setField(request, "content", "수정된 내용");
        setField(request, "imageUrl", "https://s3.amazonaws.com/test.jpg");
        setField(request, "ingredients", List.of(Map.of("name", "두부")));
        setField(request, "steps", List.of(Map.of("step", "1. 두부를 썬다")));

        given(communityPostRepository.findById(1L)).willReturn(Optional.of(mockPost));

        // when & then
        assertThatThrownBy(() -> communityService.updatePost(1L, request, "other@test.com"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("본인 게시물 삭제 시 정상 삭제된다")
    void deletePost_success() {
        // given
        given(communityPostRepository.findById(1L)).willReturn(Optional.of(mockPost));

        // when & then
        assertThatCode(() -> communityService.deletePost(1L, "test@test.com"))
                .doesNotThrowAnyException();

        then(communityPostRepository).should().delete(mockPost);
    }

    @Test
    @DisplayName("타인 게시물 삭제 시 UNAUTHORIZED 예외가 발생한다")
    void deletePost_unauthorized() {
        // given
        given(communityPostRepository.findById(1L)).willReturn(Optional.of(mockPost));

        // when & then
        assertThatThrownBy(() -> communityService.deletePost(1L, "other@test.com"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("좋아요 누르지 않은 게시물에 좋아요 시 PostLike가 저장된다")
    void toggleLike_like() {
        // given
        given(communityPostRepository.findById(1L)).willReturn(Optional.of(mockPost));
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(mockUser));
        given(postLikeRepository.existsByPostAndUser(mockPost, mockUser)).willReturn(false);

        // when
        communityService.toggleLike(1L, "test@test.com");

        // then
        then(postLikeRepository).should().save(any(PostLike.class));
    }

    @Test
    @DisplayName("이미 좋아요 누른 게시물에 좋아요 시 PostLike가 삭제된다")
    void toggleLike_unlike() {
        // given
        given(communityPostRepository.findById(1L)).willReturn(Optional.of(mockPost));
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(mockUser));
        given(postLikeRepository.existsByPostAndUser(mockPost, mockUser)).willReturn(true);

        // when
        communityService.toggleLike(1L, "test@test.com");

        // then
        then(postLikeRepository).should().deleteByPostAndUser(mockPost, mockUser);
    }

    @Test
    @DisplayName("댓글 작성 시 CommentResponse를 반환한다")
    void createComment_success() throws Exception {
        // given
        CommentCreateRequest request = new CommentCreateRequest();
        setField(request, "content", "맛있겠다!");

        given(communityPostRepository.findById(1L)).willReturn(Optional.of(mockPost));
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(mockUser));
        given(commentRepository.save(any(Comment.class))).willReturn(mockComment);

        // when
        CommentResponse result = communityService.createComment(1L, request, "test@test.com");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("맛있겠다!");
    }

    @Test
    @DisplayName("댓글 목록 조회 시 List<CommentResponse>를 반환한다")
    void getComments_success() {
        // given
        given(communityPostRepository.findById(1L)).willReturn(Optional.of(mockPost));
        given(commentRepository.findAllByPostOrderByCreatedAtAsc(mockPost))
                .willReturn(List.of(mockComment));

        // when
        List<CommentResponse> result = communityService.getComments(1L);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("맛있겠다!");
    }

    @Test
    @DisplayName("본인 댓글 삭제 시 정상 삭제된다")
    void deleteComment_success() {
        // given
        given(commentRepository.findById(1L)).willReturn(Optional.of(mockComment));

        // when & then
        assertThatCode(() -> communityService.deleteComment(1L, "test@test.com"))
                .doesNotThrowAnyException();

        then(commentRepository).should().delete(mockComment);
    }

    @Test
    @DisplayName("타인 댓글 삭제 시 UNAUTHORIZED 예외가 발생한다")
    void deleteComment_unauthorized() {
        // given
        given(commentRepository.findById(1L)).willReturn(Optional.of(mockComment));

        // when & then
        assertThatThrownBy(() -> communityService.deleteComment(1L, "other@test.com"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);
    }

    // 리플렉션으로 private 필드에 값 주입하는 헬퍼 메서드
    // @Builder 없는 DTO를 테스트에서 생성할 때 사용
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}