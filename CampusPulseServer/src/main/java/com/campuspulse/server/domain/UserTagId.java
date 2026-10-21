package com.campuspulse.server.domain;

import java.io.Serializable;
import java.util.Objects;

public class UserTagId implements Serializable {

    private Long userId;
    private Integer tagId;

    public UserTagId() {
    }

    public UserTagId(Long userId, Integer tagId) {
        this.userId = userId;
        this.tagId = tagId;
    }

    public Long getUserId() {
        return userId;
    }

    public Integer getTagId() {
        return tagId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserTagId other)) {
            return false;
        }
        return Objects.equals(userId, other.userId) && Objects.equals(tagId, other.tagId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, tagId);
    }
}
