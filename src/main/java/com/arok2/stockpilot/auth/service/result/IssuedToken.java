package com.arok2.stockpilot.auth.service.result;

/**
 * 로그인 결과로 발급된 액세스 토큰. API 응답 형태(LoginResponse: tokenType 등)는
 * 표현 계층의 관심사이므로 여기서는 토큰과 만료만 다룬다.
 */
public record IssuedToken(String accessToken, long expiresInSeconds) {
}
