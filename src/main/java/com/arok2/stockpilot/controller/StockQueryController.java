package com.arok2.stockpilot.controller;

import com.arok2.stockpilot.dto.StockDetailResponse;
import com.arok2.stockpilot.dto.StockSummaryResponse;
import com.arok2.stockpilot.service.StockQueryService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
public class StockQueryController {

    private final StockQueryService stockQueryService;

    public StockQueryController(StockQueryService stockQueryService) {
        this.stockQueryService = stockQueryService;
    }

    /** 종목 목록 + 현재가(공개). q가 있으면 이름/코드로 필터링(검색). */
    @GetMapping
    public ResponseEntity<List<StockSummaryResponse>> list(
            @RequestParam(name = "q", required = false) String q) {
        return ResponseEntity.ok(stockQueryService.getAll(q));
    }

    /** 종목 상세 + 투자지표(공개). */
    @GetMapping("/{code}")
    public ResponseEntity<StockDetailResponse> detail(@PathVariable String code) {
        return ResponseEntity.ok(stockQueryService.getByCode(code));
    }
}
