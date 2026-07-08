package com.arok2.stockpilot.price.cache;

/**
 * 종목별 최신가 캐시. 조회 성능이 중요한 최신 시세를 저장한다(Redis 구현).
 */
public interface LatestPriceCache {

    void update(String code, long price);

    /** 최신가. 없으면 null. */
    Long get(String code);
}
