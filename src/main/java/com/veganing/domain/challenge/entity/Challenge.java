package com.veganing.domain.challenge.entity;

import com.veganing.domain.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "challenges")
@Getter
@NoArgsConstructor (access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Challenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY) // user 정보를 실제 쓸 때만 db 조회. / 성능 최적화
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 30)
    private String type;

    @Column(nullable = false)
    private Integer durationDays;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ONGOING";

    @Column(nullable = false)
    @Builder.Default
    private int currentDay = 1;

    @Column(nullable = false)
    private LocalDateTime staredAt;

    private LocalDateTime endedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
