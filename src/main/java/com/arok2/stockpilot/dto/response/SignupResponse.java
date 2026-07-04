package com.arok2.stockpilot.dto.response;

import com.arok2.stockpilot.domain.User;

public record SignupResponse(
        Long id,
        String email,
        String nickname
) {
    public static SignupResponse from(User user) {
        return new SignupResponse(user.getId(), user.getEmail(), user.getNickname());
    }
}
