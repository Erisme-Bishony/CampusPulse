package com.campuspulse.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_tag")
@IdClass(UserTagId.class)
public class UserTag {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "tag_id")
    private Integer tagId;

    protected UserTag() {
    }

    public UserTag(Long userId, Integer tagId) {
        this.userId = userId;
        this.tagId = tagId;
    }

    public Long getUserId() {
        return userId;
    }

    public Integer getTagId() {
        return tagId;
    }
}
