package com.campuspulse.server.domain;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ActivityTagId implements Serializable {

    @Column(name = "activity_id")
    private Long activityId;

    @Column(name = "tag_id")
    private Integer tagId;

    public ActivityTagId() {
    }

    public ActivityTagId(Long activityId, Integer tagId) {
        this.activityId = activityId;
        this.tagId = tagId;
    }

    public Long getActivityId() {
        return activityId;
    }

    public Integer getTagId() {
        return tagId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ActivityTagId that)) {
            return false;
        }
        return Objects.equals(activityId, that.activityId) && Objects.equals(tagId, that.tagId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(activityId, tagId);
    }
}
