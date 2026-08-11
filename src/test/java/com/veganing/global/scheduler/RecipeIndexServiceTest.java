package com.veganing.global.scheduler;

import com.veganing.domain.community.entity.CommunityPost;
import com.veganing.domain.recipe.repository.RecipeRepository;
import com.veganing.global.infra.voyage.VoyageAiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;

// ─────────────────────────────────────────────────────────────
// RecipeIndexService 단위테스트
//
// 검증 핵심:
//   1. 정상 인덱싱: 새 게시물 → Voyage 임베딩 → DB 저장
//   2. 중복 skip:  이미 인덱싱된 게시물은 embed/저장 모두 건너뜀
//   3. 장애 격리:  Voyage API 실패 or DB 저장 실패 시
//                 예외가 호출자(스케줄러)로 전파되지 않아야 함
//                 → 게시물 하나 실패해도 다른 게시물 인덱싱은 계속돼야 함
//
// RecipeIndexService가 별도 클래스로 분리된 이유:
//   RecipeScheduler에서 같은 클래스 내부 메서드를 호출하면
//   @Transactional 프록시를 우회해 트랜잭션이 걸리지 않음.
//   외부 클래스 호출이 되어야 프록시를 거쳐 @Transactional이 동작함.
//   (MealAsyncService 분리와 동일한 패턴)
// ─────────────────────────────────────────────────────────────
@ExtendWith(MockitoExtension.class)
class RecipeIndexServiceTest {

    // @InjectMocks: 생성자/필드 주입으로 Mock을 자동 연결
    // RecipeIndexService는 생성자에서 외부 API를 직접 호출하지 않으므로
    // VoyageAiService와 달리 @InjectMocks 정상 사용 가능
    @InjectMocks
    private RecipeIndexService recipeIndexService;

    @Mock private RecipeRepository recipeRepository;
    @Mock private VoyageAiService voyageAiService;

    private CommunityPost mockPost;

    @BeforeEach
    void setUp() {
        // 테스트용 CommunityPost (DB 저장 없이 메모리에만 존재)
        // title + content를 합쳐서 Voyage AI에 임베딩 요청 → 두 필드 모두 필요
        mockPost = CommunityPost.builder()
                .title("두부 스크램블 레시피")
                .content("두부를 으깨서 강황, 소금과 함께 볶아주세요")
                .build();
    }

    @Test
    @DisplayName("새로운 게시물은 임베딩 생성 후 recipes 테이블에 저장된다")
    void indexSingleRecipe_success() {
        // given
        // existsByPostId = false → 아직 인덱싱 안 된 새 게시물
        given(recipeRepository.existsByPostId(any())).willReturn(false);
        // Voyage AI가 512차원 벡터를 반환한다고 가정 (테스트에선 3개로 축약)
        given(voyageAiService.embed(any())).willReturn(new float[]{0.1f, 0.2f, 0.3f});

        // when
        recipeIndexService.indexSingleRecipe(mockPost);

        // then
        // 새 게시물이므로 embed가 반드시 1회 호출되어야 함
        then(voyageAiService).should().embed(any());
        // 임베딩 결과가 DB에 저장되어야 함 (postId, title, content, embeddingStr 4개 인자)
        then(recipeRepository).should().insertWithEmbedding(any(), any(), any(), any());
    }

    @Test
    @DisplayName("이미 인덱싱된 게시물은 임베딩 생성 없이 skip된다")
    void indexSingleRecipe_alreadyIndexed_skip() {
        // given
        // existsByPostId = true → 이미 recipes 테이블에 row가 있음
        // 같은 게시물을 중복 인덱싱하면 동일 벡터가 쌓여 검색 품질 저하
        given(recipeRepository.existsByPostId(any())).willReturn(true);

        // when
        recipeIndexService.indexSingleRecipe(mockPost);

        // then
        // skip 됐으므로 Voyage API 호출 없어야 함 (불필요한 API 비용 방지)
        then(voyageAiService).should(never()).embed(any());
        // DB 저장도 없어야 함
        then(recipeRepository).should(never()).insertWithEmbedding(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Voyage AI 호출 실패 시 예외가 외부로 전파되지 않는다")
    void indexSingleRecipe_voyageApiFails_noException() {
        // given
        given(recipeRepository.existsByPostId(any())).willReturn(false);
        // Voyage AI 서버 장애, 네트워크 오류 등 RuntimeException 시뮬레이션
        given(voyageAiService.embed(any())).willThrow(new RuntimeException("Voyage AI 연결 실패"));

        // when & then
        // indexSingleRecipe 내부에 try-catch가 있어 예외를 잡고 로그만 남김
        // 예외가 전파되면 RecipeScheduler가 중단되고 나머지 게시물 인덱싱도 모두 실패함
        // → 한 게시물 실패가 전체 스케줄러를 멈춰선 안 됨
        assertThatNoException().isThrownBy(() ->
                recipeIndexService.indexSingleRecipe(mockPost)
        );

        // Voyage API가 실패했으므로 embeddingStr을 만들 수 없어 DB 저장도 없어야 함
        then(recipeRepository).should(never()).insertWithEmbedding(any(), any(), any(), any());
    }

    @Test
    @DisplayName("DB 저장 실패 시에도 예외가 외부로 전파되지 않는다")
    void indexSingleRecipe_dbSaveFails_noException() {
        // given
        given(recipeRepository.existsByPostId(any())).willReturn(false);
        // Voyage API는 성공 → embeddingStr 생성까지는 정상 진행
        given(voyageAiService.embed(any())).willReturn(new float[]{0.1f, 0.2f});
        // insertWithEmbedding에서 DB 오류 발생 (네이티브 쿼리 실행 실패 등)
        willThrow(new RuntimeException("DB 저장 실패"))
                .given(recipeRepository).insertWithEmbedding(any(), any(), any(), any());

        // when & then
        // DB 실패도 try-catch로 잡혀야 하므로 예외 전파 없어야 함
        assertThatNoException().isThrownBy(() ->
                recipeIndexService.indexSingleRecipe(mockPost)
        );
    }
}