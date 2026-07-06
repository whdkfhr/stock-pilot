package com.arok2.stockpilot.security;

import com.arok2.stockpilot.config.JwtProperties;

import io.jsonwebtoken.JwtException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JWT 발급/검증 로직 단위 테스트 (Spring 컨텍스트 없이).
 * HS256 키 요건(32바이트 이상)을 충족하는 비밀키를 사용한다.
 */
class JwtTokenProviderTest {

    private static final String SECRET = "unit-test-secret-key-that-is-long-enough-32b";

    private JwtTokenProvider provider(long ttlSeconds) {
        return new JwtTokenProvider(new JwtProperties(SECRET, ttlSeconds));
    }

    @Test
    void 발급한_토큰에서_사용자_식별자를_복원할_수_있다() {
        JwtTokenProvider provider = provider(3600);
        String token = provider.createAccessToken(42L, "user@example.com");

        assertThat(provider.isValid(token)).isTrue();
        assertThat(provider.parseUserId(token)).isEqualTo(42L);
    }

    @Test
    void 만료된_토큰은_유효하지_않다() {
        JwtTokenProvider provider = provider(-1); // 발급 즉시 만료
        String token = provider.createAccessToken(1L, "user@example.com");

        assertThat(provider.isValid(token)).isFalse();
        assertThatThrownBy(() -> provider.parseUserId(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void 다른_비밀키로_서명된_토큰은_유효하지_않다() {
        String token = provider(3600).createAccessToken(1L, "user@example.com");
        JwtTokenProvider otherKeyProvider = new JwtTokenProvider(
                new JwtProperties("a-completely-different-secret-key-32bytes!", 3600));

        assertThat(otherKeyProvider.isValid(token)).isFalse();
    }

    @Test
    void 위조된_토큰은_유효하지_않다() {
        String token = provider(3600).createAccessToken(1L, "user@example.com");
        String tampered = token.substring(0, token.length() - 2) + "zz";

        assertThat(provider(3600).isValid(tampered)).isFalse();
    }
}
