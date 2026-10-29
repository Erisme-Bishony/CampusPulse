package com.campuspulse.server.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "recommendation")
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "activity_id", nullable = false)
    private Long activityId;

    @Column(nullable = false, precision = 6, scale = 4)
    private BigDecimal score;

    @Column(nullable = false, length = 255)
    private String reason;

    @Column(nullable = false, length = 30)
    private String algorithm;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    protected Recommendation() {
    }

    public Recommendation(Long userId, Long activityId, BigDecimal score, String reason,
                          String algorithm, LocalDateTime generatedAt) {
        this.userId = userId;
        this.activityId = activityId;
        this.score = score;
        this.reason = reason;
        this.algorithm = algorithm;
        this.generatedAt = generatedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getActivityId() {
        return activityId;
    }

    public BigDecimal getScore() {
        return score;
    }

    public String getReason() {
        return reason;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }
}
