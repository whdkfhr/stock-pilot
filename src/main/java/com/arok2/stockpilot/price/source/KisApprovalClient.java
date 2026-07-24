package com.arok2.stockpilot.price.source;

import com.arok2.stockpilot.price.YahooHttp;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * KIS WebSocket 접속용 approval_key 발급. REST 토큰과 별개이며, WS 연결/재연결 시 사용한다.
 * {@code stockpilot.kis.websocket-enabled=true}일 때만 활성화.
 */
@Component
@ConditionalOnProperty(name = "stockpilot.kis.websocket-enabled", havingValue = "true")
public class KisApprovalClient {

    private final RestClient restClient;
    private final KisProperties properties;
    private final ObjectMapper objectMapper;

    public KisApprovalClient(KisProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(YahooHttp.timeoutFactory())
                .build();
    }

    public String getApprovalKey() {
        String body = restClient.post()
                .uri("/oauth2/Approval")
                .header("content-type", "application/json")
                .body(Map.of(
                        "grant_type", "client_credentials",
                        "appkey", properties.getAppKey(),
                        "secretkey", properties.getAppSecret())) // Approval은 secretkey 필드명 사용
                .retrieve()
                .body(String.class);
        try {
            JsonNode node = objectMapper.readTree(body);
            String key = node.path("approval_key").asText(null);
            if (key == null) {
                throw new IllegalStateException("approval_key 발급 실패: " + body);
            }
            return key;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("approval_key 응답 파싱 실패: " + e.getMessage(), e);
        }
    }
}
