package com.arok2.stockpilot.watchlist.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WatchlistTest {

    @Test
    void 관심등록은_등록시각을_스스로_기록한다() {
        Watchlist watchlist = Watchlist.register(1L, 42L);

        assertThat(watchlist.getUserId()).isEqualTo(1L);
        assertThat(watchlist.getStockId()).isEqualTo(42L);
        assertThat(watchlist.getCreatedAt()).isNotNull();
    }

    @Test
    void 사용자나_종목이_없으면_등록할_수_없다() {
        assertThatThrownBy(() -> Watchlist.register(null, 42L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Watchlist.register(1L, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 소유자와_대상종목을_스스로_판단한다() {
        Watchlist watchlist = Watchlist.register(1L, 42L);

        assertThat(watchlist.isOwnedBy(1L)).isTrue();
        assertThat(watchlist.isOwnedBy(2L)).isFalse();
        assertThat(watchlist.isFor(42L)).isTrue();
        assertThat(watchlist.isFor(99L)).isFalse();
    }
}
