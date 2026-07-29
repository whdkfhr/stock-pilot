package com.arok2.stockpilot.price.stream;

import com.arok2.stockpilot.price.domain.PriceChange;

import com.arok2.stockpilot.price.event.StockPriceEvent;

/** SSE로 프론트에 밀어줄 경량 시세 틱. 통화는 프론트가 초기 목록에서 이미 알고 있다. */
public record PriceTick(String code, long price, Long change, Double changePercent) {

    public static PriceTick from(StockPriceEvent e) {
        PriceChange change = PriceChange.of(e.price(), e.previousClose());
        return new PriceTick(e.code(), e.price(),
                PriceChange.changeOrNull(change), PriceChange.percentOrNull(change));
    }
}
