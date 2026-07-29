package com.arok2.stockpilot.user.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class RiskProfileTest {

    @ParameterizedTest
    @EnumSource(RiskProfile.class)
    void 모든_성향의_가중치_합은_1이다(RiskProfile profile) {
        RiskProfile.Weights w = profile.weights();

        assertThat(w.per() + w.pbr() + w.roe() + w.dividend()).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void 공격형은_성장성_가중치가_가장_높다() {
        RiskProfile.Weights w = RiskProfile.AGGRESSIVE.weights();

        assertThat(w.roe()).isGreaterThan(w.per());
        assertThat(w.roe()).isGreaterThan(w.pbr());
        assertThat(w.roe()).isGreaterThan(w.dividend());
    }

    @Test
    void 배당형은_배당_가중치가_가장_높다() {
        RiskProfile.Weights w = RiskProfile.DIVIDEND.weights();

        assertThat(w.dividend()).isGreaterThan(w.roe());
        assertThat(w.dividend()).isGreaterThan(w.per());
        assertThat(w.dividend()).isGreaterThan(w.pbr());
    }

    @Test
    void 안정형은_가치지표_비중이_성장성보다_크다() {
        RiskProfile.Weights w = RiskProfile.STABLE.weights();

        assertThat(w.per() + w.pbr()).isGreaterThan(w.roe() + w.dividend());
    }
}
