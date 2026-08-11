package com.veganing.domain.carbon.entity;

import com.veganing.domain.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "carbon_daily", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "carbon_date"})
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CarbonDaily {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "carbon_date")
    private LocalDate carbonDate;

    // 하루 총 절감량
    @Column(precision = 10, scale = 4)
    private BigDecimal totalCarbon;

    // 당일 식단 수
    @Column
    @Builder.Default
    private Integer mealCount = 0;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // 식단 추가될 때마다 탄소량 누적 + 식단 수 증가
    public void addCarbon(BigDecimal carbon) {
        this.totalCarbon = this.totalCarbon.add(carbon);
        this.mealCount += 1;
    }
}
