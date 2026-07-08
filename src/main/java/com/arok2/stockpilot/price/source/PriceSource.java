package com.arok2.stockpilot.price.source;

import com.arok2.stockpilot.price.event.StockPriceEvent;

/**
 * 시세 공급원 추상화. 운영에서는 외부 Open API(한국투자증권/Yahoo 등) 구현으로 교체한다.
 * 테스트·데모에서는 목 구현을 사용한다.
 */
public interface PriceSource {

    StockPriceEvent fetch(String code);
}
