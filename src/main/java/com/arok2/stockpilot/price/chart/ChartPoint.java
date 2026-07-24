package com.arok2.stockpilot.price.chart;

import java.time.Instant;

/** 차트 한 봉(OHLCV). */
public record ChartPoint(
        Instant time,
        double open,
        double high,
        double low,
        double close,
        long volume
) {
}
