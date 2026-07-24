package com.arok2.stockpilot.price.quote;

/**
 * 종목 시세 요약(고가/저가/거래량/52주 최고·최저 + 영문명/거래소).
 * 가격은 통화의 정수 단위로 반올림. 값이 없으면 null.
 */
public record QuoteResponse(
        String code,
        String currency,
        Long dayHigh,
        Long dayLow,
        Long volume,
        Long week52High,
        Long week52Low,
        String name,
        String exchange
) {
}
