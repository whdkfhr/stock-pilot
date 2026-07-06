package com.arok2.stockpilot.security;

import com.arok2.stockpilot.config.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

/**
 * JWT 액세스 토큰의 발급과 검증을 담당한다.
 * 서명 알고리즘은 HS256이며, 비밀키는 설정값(stockpilot.jwt.secret)에서 파생한다.
 */
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long validitySeconds;

    public JwtTokenProvider(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.validitySeconds = properties.accessTokenValiditySeconds();
    }

    /** 사용자 식별자를 subject로, 이메일을 클레임으로 담은 액세스 토큰을 발급한다. */
    public String createAccessToken(Long userId, String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + Duration.ofSeconds(validitySeconds).toMillis());
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public long getValiditySeconds() {
        return validitySeconds;
    }

    /** 서명·만료를 검증하고 사용자 식별자를 반환한다. 유효하지 않으면 JwtException을 던진다. */
    public Long parseUserId(String token) {
        return Long.valueOf(parse(token).getSubject());
    }

    /** 토큰이 유효하면 true, 서명 위조·만료·형식 오류면 false. */
    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
