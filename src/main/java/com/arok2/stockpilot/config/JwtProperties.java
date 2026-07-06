package com.arok2.stockpilot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 관련 설정값 (application.yml의 stockpilot.jwt.*).
 */
@ConfigurationProperties(prefix = "stockpilot.jwt")
public record JwtProperties(
        String secret,
        long accessTokenValiditySeconds
) {
}
