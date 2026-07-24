package com.arok2.stockpilot.price.source;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 한국투자증권(KIS) OpenAPI 설정. 앱키/시크릿은 절대 코드에 두지 않고 환경변수로 주입한다.
 * base-url: 실전 https://openapi.koreainvestment.com:9443 / 모의 https://openapivts.koreainvestment.com:29443
 */
@ConfigurationProperties(prefix = "stockpilot.kis")
public class KisProperties {

    private String appKey;
    private String appSecret;
    private String baseUrl = "https://openapi.koreainvestment.com:9443";
    // 실시간 체결가 WebSocket. 실전 ws://ops.koreainvestment.com:21000 / 모의 :31000
    private String wsUrl = "ws://ops.koreainvestment.com:21000";
    private boolean websocketEnabled = false;

    public String getWsUrl() {
        return wsUrl;
    }

    public void setWsUrl(String wsUrl) {
        this.wsUrl = wsUrl;
    }

    public boolean isWebsocketEnabled() {
        return websocketEnabled;
    }

    public void setWebsocketEnabled(boolean websocketEnabled) {
        this.websocketEnabled = websocketEnabled;
    }

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
