package com.arok2.stockpilot.price.stream;

import com.arok2.stockpilot.price.event.StockPriceEvent;

/** SSE로 프론트에 밀어줄 경량 시세 틱. 통화는 프론트가 초기 목록에서 이미 알고 있다. */
public record PriceTick(String code, long price, Long change, Double changePercent) {

    public static PriceTick from(StockPriceEvent e) {
        long prev = e.previousClose();
        Long change = null;
        Double changePercent = null;
        if (prev != 0) {
            change = e.price() - prev;
            changePercent = Math.round((change * 10000.0) / prev) / 100.0;
        }
        return new PriceTick(e.code(), e.price(), change, changePercent);
    }
}
