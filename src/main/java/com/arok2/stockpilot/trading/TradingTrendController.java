package com.arok2.stockpilot.trading;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stocks")
public class TradingTrendController {

    private final TradingTrendService tradingTrendService;

    public TradingTrendController(TradingTrendService tradingTrendService) {
        this.tradingTrendService = tradingTrendService;
    }

    /** 투자자별 매매동향(공개). 국내 종목만 샘플 데이터 제공. */
    @GetMapping("/{code}/trading-trend")
    public ResponseEntity<TradingTrendResponse> tradingTrend(@PathVariable String code) {
        return ResponseEntity.ok(tradingTrendService.getTrend(code));
    }
}
