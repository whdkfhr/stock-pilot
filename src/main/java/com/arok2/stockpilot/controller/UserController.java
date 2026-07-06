package com.arok2.stockpilot.controller;

import com.arok2.stockpilot.dto.response.MeResponse;
import com.arok2.stockpilot.service.UserService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** JWT로 인증된 현재 사용자 정보를 반환한다. principal은 사용자 식별자(Long). */
    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(userService.getMe(userId));
    }
}
