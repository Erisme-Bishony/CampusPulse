package com.campuspulse.server.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "behavior")
public class Behavior {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "activity_id", nullable = false)
    private Long activityId;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(name = "duration_seconds", nullable = false)
    private Integer durationSeconds = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Behavior() {
    }

    public Behavior(Long userId, Long activityId, String type, Integer durationSeconds, LocalDateTime createdAt) {
        this.userId = userId;
        this.activityId = activityId;
        this.type = type;
        this.durationSeconds = durationSeconds == null ? 0 : durationSeconds;
        this.createdAt = createdAt;
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

    public String getType() {
        return type;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
