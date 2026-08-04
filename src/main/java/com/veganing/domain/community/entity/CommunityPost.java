package com.veganing.domain.community.entity;

import com.veganing.domain.auth.entity.User;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "community_posts")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CommunityPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    // 자유 설명 (선택)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // S3 이미지 URL (필수)
    @Column(nullable = false)
    private String imageUrl;

    // 재료 목록 (필수)
    // [{"name": "두부", "amount": "150g"}, ...]
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<Map<String, String>> ingredients;

    // 레시피 순서
    // [{"step": "1", "description": "두부를 깍둑썰기한다"}, ...]
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<Map<String, String>> steps;

    @Builder.Default
    @Column
    private Integer likeCount = 0;

    @Builder.Default
    @Column
    private Integer commentCount = 0;

    // 낙관적 락
    @Version
    private Long version;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // 수정 메서드
    public void update(String title, String content, String imageUrl, List<Map<String, String>> ingredients, List<Map<String, String>> steps) {
        this.title = title;
        this.content = content;
        this.imageUrl = imageUrl;
        this.ingredients = ingredients;
        this.steps = steps;
    }

    // 좋아요 증가 메서드
    public void incrementLikeCount() {
        this.likeCount++;
    }

    // 좋아요 감소 메서드
    public void decrementLikeCount() {
        this.likeCount--;
    }

    // 댓글 증가 메서드
    public void incrementCommentCount() {
        this.commentCount++;
    }

    // 댓글 감소 메서드
    public void decrementCommentCount() {
        this.commentCount--;
    }
}