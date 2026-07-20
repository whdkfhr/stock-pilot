package com.arok2.stockpilot.price.chart;

import java.time.Instant;

/** 차트 한 점(캔들 종가). */
public record ChartPoint(Instant time, double close) {
}
