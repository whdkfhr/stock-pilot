package com.arok2.stockpilot.controller;

import com.arok2.stockpilot.dto.StockSummaryResponse;
import com.arok2.stockpilot.service.StockQueryService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
public class StockQueryController {

    private final StockQueryService stockQueryService;

    public StockQueryController(StockQueryService stockQueryService) {
        this.stockQueryService = stockQueryService;
    }

    /** 전체 종목 목록 + 현재가(공개). */
    @GetMapping
    public ResponseEntity<List<StockSummaryResponse>> list() {
        return ResponseEntity.ok(stockQueryService.getAll());
    }
}
