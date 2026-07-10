package com.arok2.stockpilot.notification.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AlertConditionTest {

    @Test
    void ABOVE_조건은_임계값_이상일_때_충족된다() {
        AlertCondition condition = AlertCondition.of(1L, "005930", AlertDirection.ABOVE, 60_000);

        assertThat(condition.matches(59_999)).isFalse();
        assertThat(condition.matches(60_000)).isTrue();
        assertThat(condition.matches(60_001)).isTrue();
    }

    @Test
    void BELOW_조건은_임계값_이하일_때_충족된다() {
        AlertCondition condition = AlertCondition.of(1L, "005930", AlertDirection.BELOW, 50_000);

        assertThat(condition.matches(50_001)).isFalse();
        assertThat(condition.matches(50_000)).isTrue();
        assertThat(condition.matches(49_999)).isTrue();
    }

    @Test
    void 생성_직후_상태는_ACTIVE이고_발화시각은_없다() {
        AlertCondition condition = AlertCondition.of(1L, "005930", AlertDirection.ABOVE, 60_000);

        assertThat(condition.getStatus()).isEqualTo(AlertStatus.ACTIVE);
        assertThat(condition.getTriggeredAt()).isNull();
        assertThat(condition.getCreatedAt()).isNotNull();
    }
}
