package com.arok2.stockpilot.price.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 전일 대비 등락 계산 규칙 검증. 응답 DTO 3곳에 복제돼 있던 규칙을 값 객체로 모은 뒤의 계약이다.
 */
class PriceChangeTest {

    @Test
    void 상승분과_등락률을_계산한다() {
        PriceChange change = PriceChange.of(53_592L, 50_000L);

        assertThat(change.change()).isEqualTo(3_592L);
        assertThat(change.percent()).isEqualTo(7.18); // 소수점 둘째 자리 반올림
        assertThat(change.isRise()).isTrue();
        assertThat(change.isFall()).isFalse();
    }

    @Test
    void 하락은_음수로_표현된다() {
        PriceChange change = PriceChange.of(1_788_000L, 1_919_000L);

        assertThat(change.change()).isEqualTo(-131_000L);
        assertThat(change.percent()).isEqualTo(-6.83);
        assertThat(change.isFall()).isTrue();
    }

    @Test
    void 보합이면_등락이_0이다() {
        PriceChange change = PriceChange.of(50_000L, 50_000L);

        assertThat(change.change()).isZero();
        assertThat(change.percent()).isZero();
        assertThat(change.isRise()).isFalse();
        assertThat(change.isFall()).isFalse();
    }

    @Test
    void 계산할_수_없으면_null이다() {
        assertThat(PriceChange.of(null, 50_000L)).isNull();   // 현재가 미수집
        assertThat(PriceChange.of(50_000L, null)).isNull();   // 전일종가 없음
        assertThat(PriceChange.of(50_000L, 0L)).isNull();     // 0으로 나눌 수 없음
    }

    @Test
    void null_헬퍼는_계산_불가를_그대로_전달한다() {
        assertThat(PriceChange.changeOrNull(null)).isNull();
        assertThat(PriceChange.percentOrNull(null)).isNull();

        PriceChange change = PriceChange.of(110L, 100L);
        assertThat(PriceChange.changeOrNull(change)).isEqualTo(10L);
        assertThat(PriceChange.percentOrNull(change)).isEqualTo(10.0);
    }
}
