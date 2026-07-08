package com.arok2.stockpilot.price.source;

import com.arok2.stockpilot.price.event.StockPriceEvent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RandomWalkPriceSourceTest {

    @Test
    void 요청한_종목의_양수_시세를_생성한다() {
        RandomWalkPriceSource source = new RandomWalkPriceSource();

        StockPriceEvent event = source.fetch("005930");

        assertThat(event.code()).isEqualTo("005930");
        assertThat(event.price()).isGreaterThan(0);
        assertThat(event.volume()).isGreaterThan(0);
        assertThat(event.timestamp()).isNotNull();
    }

    @Test
    void 연속_조회_시_직전가에서_한도_이내로_변동한다() {
        RandomWalkPriceSource source = new RandomWalkPriceSource();

        long prev = source.fetch("X").price();
        for (int i = 0; i < 20; i++) {
            long next = source.fetch("X").price();
            assertThat(Math.abs(next - prev)).isLessThanOrEqualTo(500);
            prev = next;
        }
    }
}
