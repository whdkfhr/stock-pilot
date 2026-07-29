package com.arok2.stockpilot.auth.controller;

import com.arok2.stockpilot.auth.dto.request.LoginRequest;
import com.arok2.stockpilot.auth.dto.request.SignupRequest;
import com.arok2.stockpilot.auth.dto.response.LoginResponse;
import com.arok2.stockpilot.auth.dto.response.SignupResponse;
import com.arok2.stockpilot.auth.service.AuthService;
import com.arok2.stockpilot.auth.service.result.IssuedToken;
import com.arok2.stockpilot.user.domain.User;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        User created = authService.signup(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(SignupResponse.from(created));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        IssuedToken token = authService.login(request.toCommand());
        return ResponseEntity.ok(LoginResponse.of(token.accessToken(), token.expiresInSeconds()));
    }
}
