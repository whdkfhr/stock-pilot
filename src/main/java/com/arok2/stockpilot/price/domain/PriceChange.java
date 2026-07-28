package com.arok2.stockpilot.price.domain;

/**
 * 전일 종가 대비 등락(값 객체).
 *
 * <p>등락 계산은 "현재가와 전일 종가가 모두 있어야 하고, 전일 종가가 0이면 등락률을 낼 수 없다"는
 * 도메인 규칙을 갖는다. 이 규칙이 응답 DTO마다 복제돼 있어 값 객체로 모았다.
 *
 * <p>등락률은 소수점 둘째 자리로 반올림한다(화면 표기 단위와 일치).
 */
public record PriceChange(long change, double percent) {

    /** 계산할 수 없으면(현재가 미수집·전일종가 0) {@code null}. */
    public static PriceChange of(Long price, Long previousClose) {
        if (price == null || previousClose == null || previousClose == 0) {
            return null;
        }
        long change = price - previousClose;
        double percent = Math.round((change * 10000.0) / previousClose) / 100.0;
        return new PriceChange(change, percent);
    }

    /** 등락값(없으면 null) — 응답 DTO의 nullable 필드에 그대로 쓰기 위한 헬퍼. */
    public static Long changeOrNull(PriceChange priceChange) {
        return priceChange == null ? null : priceChange.change();
    }

    /** 등락률(없으면 null). */
    public static Double percentOrNull(PriceChange priceChange) {
        return priceChange == null ? null : priceChange.percent();
    }

    public boolean isRise() {
        return change > 0;
    }

    public boolean isFall() {
        return change < 0;
    }
}
