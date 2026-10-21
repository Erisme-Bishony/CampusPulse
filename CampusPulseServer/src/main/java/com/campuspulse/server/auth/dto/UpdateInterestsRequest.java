package com.campuspulse.server.auth.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;

public record UpdateInterestsRequest(
        @NotNull(message = "tagIds 不能为空") List<@NotNull Integer> tagIds) {
}
