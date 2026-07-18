package com.arok2.stockpilot.service;

import com.arok2.stockpilot.dto.StockDetailResponse;
import com.arok2.stockpilot.dto.StockSummaryResponse;
import com.arok2.stockpilot.exception.StockNotFoundException;
import com.arok2.stockpilot.price.cache.LatestPriceCache;
import com.arok2.stockpilot.repository.StockRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 종목 목록 조회. 각 종목에 최신가 캐시(Redis)의 현재가를 붙여 요약으로 반환한다.
 */
@Service
public class StockQueryService {

    private final StockRepository stockRepository;
    private final LatestPriceCache latestPriceCache;

    public StockQueryService(StockRepository stockRepository, LatestPriceCache latestPriceCache) {
        this.stockRepository = stockRepository;
        this.latestPriceCache = latestPriceCache;
    }

    @Transactional(readOnly = true)
    public List<StockSummaryResponse> getAll(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        return stockRepository.findAll().stream()
                .filter(stock -> q.isEmpty()
                        || stock.getName().toLowerCase().contains(q)
                        || stock.getCode().toLowerCase().contains(q))
                .map(stock -> StockSummaryResponse.of(stock, latestPriceCache.get(stock.getCode())))
                .toList();
    }

    @Transactional(readOnly = true)
    public StockDetailResponse getByCode(String code) {
        return stockRepository.findByCode(code)
                .map(stock -> StockDetailResponse.of(stock, latestPriceCache.get(code)))
                .orElseThrow(() -> new StockNotFoundException(code));
    }
}
