package com.veganing.global.scheduler;

import com.veganing.domain.community.entity.CommunityPost;
import com.veganing.domain.community.repository.CommunityPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecipeScheduler {

    private final CommunityPostRepository communityPostRepository;
    private final RecipeIndexService recipeIndexService;

    @Scheduled(cron = "${scheduler.recipe-cron}")
    public void indexTopRecipes() {
        log.info("레시피 인덱싱 스케줄러 시작");

        List<CommunityPost> topPosts = communityPostRepository
                .findTopByLikeCount(PageRequest.of(0, 20));

        log.info("조회된 게시물 수: {}", topPosts.size());
        if (topPosts.isEmpty()) {
            log.info("인덱싱할 게시물 없음");
            return;
        }

        // 외부 클래스 호출 → 프록시 거침 → @Transactional 정상 동작
        for (CommunityPost post : topPosts) {
            recipeIndexService.indexSingleRecipe(post);
        }

        log.info("레시피 인덱싱 스케줄러 완료");
    }
}