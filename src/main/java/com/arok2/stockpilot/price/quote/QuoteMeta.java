package com.arok2.stockpilot.price.quote;

/** Yahoo chart meta에서 추출한 원시 요약 값(정규화 전). 없는 값은 null. */
public record QuoteMeta(
        Double dayHigh,
        Double dayLow,
        long volume,
        Double fiftyTwoWeekHigh,
        Double fiftyTwoWeekLow,
        String name,
        String exchange
) {
}
