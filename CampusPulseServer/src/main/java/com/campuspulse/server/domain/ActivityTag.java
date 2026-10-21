package com.campuspulse.server.domain;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "activity_tag")
public class ActivityTag {

    @EmbeddedId
    private ActivityTagId id;

    public ActivityTag() {
    }

    public ActivityTag(Long activityId, Integer tagId) {
        this.id = new ActivityTagId(activityId, tagId);
    }

    public ActivityTagId getId() {
        return id;
    }
}
