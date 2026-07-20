package com.arok2.stockpilot.price.cache;

/**
 * 종목별 최신가 캐시. 조회 성능이 중요한 최신 시세를 저장한다(Redis 구현).
 * 등락 계산을 위해 전일 종가(previousClose)도 함께 보관한다.
 */
public interface LatestPriceCache {

    void update(String code, long price, long previousClose);

    /** 최신가. 없으면 null. */
    Long get(String code);

    /** 전일 종가. 없으면 null. */
    Long getPreviousClose(String code);
}
