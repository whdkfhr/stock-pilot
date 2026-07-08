package com.arok2.stockpilot.price.controller;

import com.arok2.stockpilot.price.cache.LatestPriceCache;
import com.arok2.stockpilot.price.dto.LatestPriceResponse;
import com.arok2.stockpilot.price.dto.PriceHistoryResponse;
import com.arok2.stockpilot.price.repository.PriceHistoryRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 시세 조회 API (공개). 최신가는 Redis에서(Cache-Aside), 이력은 PostgreSQL에서 조회한다.
 */
@RestController
@RequestMapping("/api/stocks")
public class PriceController {

    private final LatestPriceCache latestPriceCache;
    private final PriceHistoryRepository priceHistoryRepository;

    public PriceController(LatestPriceCache latestPriceCache, PriceHistoryRepository priceHistoryRepository) {
        this.latestPriceCache = latestPriceCache;
        this.priceHistoryRepository = priceHistoryRepository;
    }

    @GetMapping("/{code}/price")
    public ResponseEntity<LatestPriceResponse> latest(@PathVariable String code) {
        Long price = latestPriceCache.get(code);
        if (price == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new LatestPriceResponse(code, price));
    }

    @GetMapping("/{code}/price/history")
    public ResponseEntity<List<PriceHistoryResponse>> history(@PathVariable String code) {
        List<PriceHistoryResponse> history = priceHistoryRepository
                .findTop20ByCodeOrderByTradedAtDesc(code).stream()
                .map(PriceHistoryResponse::from)
                .toList();
        return ResponseEntity.ok(history);
    }
}
