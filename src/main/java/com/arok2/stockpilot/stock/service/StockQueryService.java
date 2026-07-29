package com.arok2.stockpilot.stock.service;

import com.arok2.stockpilot.exception.StockNotFoundException;
import com.arok2.stockpilot.price.cache.LatestPriceCache;
import com.arok2.stockpilot.stock.repository.StockRepository;
import com.arok2.stockpilot.stock.service.result.StockDetail;
import com.arok2.stockpilot.stock.service.result.StockSummary;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 종목 조회. 각 종목에 최신가 캐시(Redis)의 현재가를 붙여 조회 결과로 반환한다.
 * API 표현(StockSummaryResponse 등) 변환은 Controller가 담당한다.
 */
@Service
public class StockQueryService {

    private final StockRepository stockRepository;
    private final LatestPriceCache latestPriceCache;

    public StockQueryService(StockRepository stockRepository, LatestPriceCache latestPriceCache) {
        this.stockRepository = stockRepository;
        this.latestPriceCache = latestPriceCache;
    }

    /** 종목 목록. {@code query}가 있으면 이름/코드로 필터링(검색). */
    @Transactional(readOnly = true)
    public List<StockSummary> getAll(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        return stockRepository.findAll().stream()
                .filter(stock -> q.isEmpty()
                        || stock.getName().toLowerCase().contains(q)
                        || stock.getCode().toLowerCase().contains(q))
                .map(stock -> StockSummary.of(stock,
                        latestPriceCache.get(stock.getCode()),
                        latestPriceCache.getPreviousClose(stock.getCode())))
                .toList();
    }

    @Transactional(readOnly = true)
    public StockDetail getByCode(String code) {
        return stockRepository.findByCode(code)
                .map(stock -> StockDetail.of(stock,
                        latestPriceCache.get(code),
                        latestPriceCache.getPreviousClose(code)))
                .orElseThrow(() -> new StockNotFoundException(code));
    }
}
