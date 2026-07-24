package com.arok2.stockpilot.price.stream;

import com.arok2.stockpilot.price.event.StockPriceEvent;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PriceTickTest {

    @Test
    void 전일종가_대비_등락을_계산한다() {
        PriceTick t = PriceTick.from(new StockPriceEvent("005930", 61000, 100, Instant.now(), 60000));

        assertThat(t.code()).isEqualTo("005930");
        assertThat(t.price()).isEqualTo(61000);
        assertThat(t.change()).isEqualTo(1000);
        assertThat(t.changePercent()).isEqualTo(1.67);
    }

    @Test
    void 전일종가가_0이면_등락은_null() {
        PriceTick t = PriceTick.from(new StockPriceEvent("005930", 61000, 100, Instant.now(), 0));

        assertThat(t.change()).isNull();
        assertThat(t.changePercent()).isNull();
    }
}
