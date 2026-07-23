package com.arok2.stockpilot.price.source;

/**
 * KIS 초당 호출 제한(EGW00201)으로 이번 조회를 건너뛴 경우. 자가치유되는 정상 상황이므로
 * 수집 스케줄러는 이 예외를 조용히(DEBUG) 처리한다.
 */
public class KisRateLimitException extends RuntimeException {

    public KisRateLimitException(String code) {
        super("KIS 초당 제한으로 스킵: " + code);
    }
}
