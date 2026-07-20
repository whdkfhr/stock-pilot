package com.arok2.stockpilot.trading;

import com.arok2.stockpilot.domain.MarketType;
import com.arok2.stockpilot.domain.Stock;
import com.arok2.stockpilot.repository.StockRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradingTrendServiceTest {

    @Mock
    private StockRepository stockRepository;

    @Test
    void 국내_종목은_개인_외국인_기관_샘플을_주고_순매수합은_0이다() {
        when(stockRepository.findByCode("005930")).thenReturn(
                Optional.of(Stock.of("005930", "삼성전자", MarketType.KOSPI, 12, 1.2, 15, 2.5)));

        TradingTrendService service = new TradingTrendService(stockRepository);
        TradingTrendResponse res = service.getTrend("005930");

        assertThat(res.sample()).isTrue();
        assertThat(res.flows()).extracting(TradingTrendResponse.InvestorFlow::investor)
                .containsExactly("개인", "외국인", "기관");
        long sum = res.flows().stream().mapToLong(TradingTrendResponse.InvestorFlow::netBuy).sum();
        assertThat(sum).isZero(); // 개인이 나머지를 흡수하도록 설계
    }

    @Test
    void 국내_종목은_같은_날_같은_코드면_안정적으로_같은_값을_준다() {
        when(stockRepository.findByCode("005930")).thenReturn(
                Optional.of(Stock.of("005930", "삼성전자", MarketType.KOSPI, 12, 1.2, 15, 2.5)));

        TradingTrendService service = new TradingTrendService(stockRepository);

        assertThat(service.getTrend("005930").flows())
                .isEqualTo(service.getTrend("005930").flows());
    }

    @Test
    void 미국_종목은_대상이_아니라_sample_false_빈목록() {
        when(stockRepository.findByCode("AAPL")).thenReturn(
                Optional.of(Stock.of("AAPL", "애플", MarketType.NASDAQ, 30, 8, 30, 0.5)));

        TradingTrendService service = new TradingTrendService(stockRepository);
        TradingTrendResponse res = service.getTrend("AAPL");

        assertThat(res.sample()).isFalse();
        assertThat(res.flows()).isEmpty();
    }
}
