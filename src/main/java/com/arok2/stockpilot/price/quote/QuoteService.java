package com.arok2.stockpilot.price.quote;

import com.arok2.stockpilot.domain.Stock;
import com.arok2.stockpilot.exception.StockNotFoundException;
import com.arok2.stockpilot.price.chart.YahooChartClient;
import com.arok2.stockpilot.price.source.YahooProperties;
import com.arok2.stockpilot.price.source.YahooSymbolResolver;
import com.arok2.stockpilot.repository.StockRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 종목 시세 요약 제공. 종목의 시장으로 Yahoo 심볼을 만들고 chart meta에서 요약 값을 가져온다.
 */
@Service
public class QuoteService {

    private final StockRepository stockRepository;
    private final YahooChartClient yahooChartClient;
    private final YahooSymbolResolver symbolResolver;

    public QuoteService(StockRepository stockRepository,
                        YahooChartClient yahooChartClient,
                        YahooProperties yahooProperties) {
        this.stockRepository = stockRepository;
        this.yahooChartClient = yahooChartClient;
        this.symbolResolver = new YahooSymbolResolver(yahooProperties);
    }

    @Transactional(readOnly = true)
    public QuoteResponse getQuote(String code) {
        Stock stock = stockRepository.findByCode(code)
                .orElseThrow(() -> new StockNotFoundException(code));
        String currency = stock.getMarket().currency();
        QuoteMeta meta = yahooChartClient.fetchMeta(symbolResolver.resolve(code, stock.getMarket()));
        if (meta == null) {
            return new QuoteResponse(code, currency, null, null, null, null, null, null, null);
        }
        return new QuoteResponse(
                code, currency,
                round(meta.dayHigh()), round(meta.dayLow()),
                meta.volume() == 0 ? null : meta.volume(),
                round(meta.fiftyTwoWeekHigh()), round(meta.fiftyTwoWeekLow()),
                meta.name(), meta.exchange());
    }

    private static Long round(Double v) {
        return v == null ? null : Math.round(v);
    }
}
