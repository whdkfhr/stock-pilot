package com.arok2.stockpilot.auth.dto.response;

import com.arok2.stockpilot.user.domain.User;

public record SignupResponse(
        Long id,
        String email,
        String nickname
) {
    public static SignupResponse from(User user) {
        return new SignupResponse(user.getId(), user.getEmail(), user.getNickname());
    }
}
