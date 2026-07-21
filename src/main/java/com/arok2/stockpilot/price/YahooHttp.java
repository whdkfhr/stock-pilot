package com.arok2.stockpilot.price;

import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.time.Duration;

/** Yahoo 호출용 공통 HTTP 설정. */
public final class YahooHttp {

    private YahooHttp() {
    }

    /**
     * 연결/읽기 타임아웃을 건 RequestFactory. 응답 없는 요청이 단일 스케줄러 스레드를
     * 영구 블로킹해 시세 수집 전체를 멈추는 것을 방지한다.
     */
    public static SimpleClientHttpRequestFactory timeoutFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        return factory;
    }
}
