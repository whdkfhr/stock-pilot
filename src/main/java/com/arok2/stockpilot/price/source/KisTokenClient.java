package com.arok2.stockpilot.price.source;

import com.arok2.stockpilot.price.YahooHttp;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * KIS OAuth 접근토큰 발급·캐시. KIS는 토큰을 24시간 유효하게 발급하고 잦은 재발급을 제한하므로,
 * 만료 전까지 메모리에 캐시해 재사용한다. {@code stockpilot.price.source=kis}일 때만 활성화.
 */
@Component
@ConditionalOnProperty(name = "stockpilot.price.source", havingValue = "kis")
public class KisTokenClient {

    private static final Logger log = LoggerFactory.getLogger(KisTokenClient.class);

    private final RestClient restClient;
    private final KisProperties properties;
    private final ObjectMapper objectMapper;

    private volatile String token;
    private volatile Instant expiresAt = Instant.MIN;

    public KisTokenClient(KisProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(YahooHttp.timeoutFactory())
                .build();
    }

    /** 유효한 토큰을 반환한다(만료 임박 시 재발급). */
    public synchronized String getToken() {
        if (token != null && Instant.now().isBefore(expiresAt)) {
            return token;
        }
        issue();
        return token;
    }

    private void issue() {
        String body = restClient.post()
                .uri("/oauth2/tokenP")
                .header("content-type", "application/json")
                .body(Map.of(
                        "grant_type", "client_credentials",
                        "appkey", properties.getAppKey(),
                        "appsecret", properties.getAppSecret()))
                .retrieve()
                .body(String.class);
        try {
            JsonNode node = objectMapper.readTree(body);
            String accessToken = node.path("access_token").asText(null);
            if (accessToken == null) {
                throw new IllegalStateException("KIS 토큰 발급 실패: " + body);
            }
            long expiresIn = node.path("expires_in").asLong(86400);
            this.token = accessToken;
            // 만료 10분 전을 유효 한계로 둔다.
            this.expiresAt = Instant.now().plusSeconds(expiresIn).minus(Duration.ofMinutes(10));
            log.info("KIS 접근토큰 발급 완료 (만료까지 {}초)", expiresIn);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("KIS 토큰 응답 파싱 실패: " + e.getMessage(), e);
        }
    }
}
