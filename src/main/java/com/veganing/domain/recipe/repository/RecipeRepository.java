package com.veganing.domain.recipe.repository;

import com.veganing.domain.community.entity.CommunityPost;
import com.veganing.domain.recipe.entity.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    // 임베딩이 저장된 레시피만 조회 (RAG 검색 대상)
    // @Convert 필드는 JPQL IS NOT NULL 인식 불가 → 네이티브 쿼리로 처리
    @Query(value = "SELECT * FROM recipes WHERE embedding IS NOT NULL", nativeQuery = true)
    List<Recipe> findAllWithEmbedding();

    // pgvector 코사인 유사도 검색 (네이티브 쿼리)
    // <=> 연산자 = pgvector 코사인 거리 (0에 가까울수록 유사)
    @Query(value = """
            SELECT * FROM recipes
            WHERE embedding IS NOT NULL
            ORDER BY embedding <=> CAST(:queryVector AS vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<Recipe> findSimilarRecipes(@Param("queryVector") String queryVector,
                                    @Param("limit") int limit);

    // post_id로 이미 인덱싱된 레시피 존재 여부 확인
    boolean existsByPost(CommunityPost post);

    // float[] → vector 변환을 DB 레벨에서 처리
// JPA 컨버터 우회, 네이티브 쿼리로 직접 INSERT
    @Modifying
    @Query(value = """
        INSERT INTO recipes (post_id, title, content, embedding, created_at)
        VALUES (:postId, :title, :content, CAST(:embedding AS vector), NOW())
        """, nativeQuery = true)
    void insertWithEmbedding(@Param("postId") Long postId,
                             @Param("title") String title,
                             @Param("content") String content,
                             @Param("embedding") String embedding);

    boolean existsByPostId(Long postId);
}