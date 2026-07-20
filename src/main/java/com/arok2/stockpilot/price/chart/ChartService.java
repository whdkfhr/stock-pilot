package com.arok2.stockpilot.price.chart;

import com.arok2.stockpilot.domain.Stock;
import com.arok2.stockpilot.exception.StockNotFoundException;
import com.arok2.stockpilot.price.source.YahooProperties;
import com.arok2.stockpilot.price.source.YahooSymbolResolver;
import com.arok2.stockpilot.repository.StockRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 종목의 기간별 차트를 제공한다. 종목의 시장으로 Yahoo 심볼을 만들고, 기간에 맞는
 * range/interval로 과거 캔들을 조회한다.
 */
@Service
public class ChartService {

    private final StockRepository stockRepository;
    private final YahooChartClient yahooChartClient;
    private final YahooSymbolResolver symbolResolver;

    public ChartService(StockRepository stockRepository,
                        YahooChartClient yahooChartClient,
                        YahooProperties yahooProperties) {
        this.stockRepository = stockRepository;
        this.yahooChartClient = yahooChartClient;
        this.symbolResolver = new YahooSymbolResolver(yahooProperties);
    }

    private enum Period {
        DAY("1d", "5m"),
        WEEK("5d", "30m"),
        MONTH("1mo", "1d");

        final String range;
        final String interval;

        Period(String range, String interval) {
            this.range = range;
            this.interval = interval;
        }

        static Period from(String value) {
            return switch (value == null ? "" : value.toUpperCase()) {
                case "1W", "WEEK" -> WEEK;
                case "1M", "MONTH" -> MONTH;
                default -> DAY;
            };
        }
    }

    @Transactional(readOnly = true)
    public ChartResponse getChart(String code, String period) {
        Stock stock = stockRepository.findByCode(code)
                .orElseThrow(() -> new StockNotFoundException(code));
        Period p = Period.from(period);
        String symbol = symbolResolver.resolve(code, stock.getMarket());
        List<ChartPoint> points = yahooChartClient.fetch(symbol, p.range, p.interval);
        return new ChartResponse(code, p.name(), points);
    }
}
