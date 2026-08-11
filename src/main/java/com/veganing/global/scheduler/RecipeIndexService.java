package com.veganing.global.scheduler;

import com.veganing.domain.community.entity.CommunityPost;
import com.veganing.domain.recipe.repository.RecipeRepository;
import com.veganing.global.infra.voyage.VoyageAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeIndexService {

    private final RecipeRepository recipeRepository;
    private final VoyageAiService voyageAiService;

    @Transactional
    public void indexSingleRecipe(CommunityPost post) {
        try {
            boolean alreadyIndexed = recipeRepository.existsByPostId(post.getId());
            if (alreadyIndexed) {
                log.info("이미 인덱싱된 게시물 skip: postId={}", post.getId());
                return;
            }

            String textToEmbed = post.getTitle() + " " + post.getContent();

            // Voyage AI 임베딩 생성
            float[] embedding = voyageAiService.embed(textToEmbed);

            // float[] → "[0.1,0.2,...]" 문자열 변환
            // DB에서 CAST(:embedding AS vector)로 처리 → JPA 컨버터 우회
            String embeddingStr = Arrays.toString(embedding).replace(" ", "");

            recipeRepository.insertWithEmbedding(
                    post.getId(),
                    post.getTitle(),
                    post.getContent(),
                    embeddingStr
            );

            log.info("레시피 인덱싱 완료: postId={}", post.getId());

        } catch (Exception e) {
            log.error("레시피 인덱싱 실패: postId={}, error={}", post.getId(), e.getMessage());
        }
    }
}