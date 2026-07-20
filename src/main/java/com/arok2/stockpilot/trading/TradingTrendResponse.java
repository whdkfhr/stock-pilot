package com.arok2.stockpilot.trading;

import java.util.List;

/**
 * 투자자별 매매동향(개인/외국인/기관 순매수).
 * sample=true면 데모용 합성 데이터다(개인/외국인/기관 수급은 KRX/KIS 전용 데이터로,
 * 한국투자증권 연동 시 실데이터로 교체된다). 미국 종목 등 비대상은 sample=false·빈 목록.
 */
public record TradingTrendResponse(
        String code,
        String unit,
        boolean sample,
        List<InvestorFlow> flows
) {
    public record InvestorFlow(String investor, long netBuy) {
    }
}
