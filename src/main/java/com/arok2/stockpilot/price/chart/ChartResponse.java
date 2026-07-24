package com.arok2.stockpilot.price.chart;

import java.util.List;

/** 기간별 차트 응답. */
public record ChartResponse(String code, String period, List<ChartPoint> points) {
}
