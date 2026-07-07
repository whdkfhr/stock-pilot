package com.arok2.stockpilot.service;

import com.arok2.stockpilot.domain.Stock;
import com.arok2.stockpilot.domain.Watchlist;
import com.arok2.stockpilot.dto.WatchlistCreateResponse;
import com.arok2.stockpilot.dto.WatchlistDeleteResponse;
import com.arok2.stockpilot.dto.WatchlistItemResponse;
import com.arok2.stockpilot.dto.WatchlistPageResponse;
import com.arok2.stockpilot.exception.StockNotFoundException;
import com.arok2.stockpilot.exception.WatchlistAlreadyExistsException;
import com.arok2.stockpilot.exception.WatchlistNotFoundException;
import com.arok2.stockpilot.repository.StockRepository;
import com.arok2.stockpilot.repository.WatchlistRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final StockRepository stockRepository;

    public WatchlistService(WatchlistRepository watchlistRepository, StockRepository stockRepository) {
        this.watchlistRepository = watchlistRepository;
        this.stockRepository = stockRepository;
    }

    @Transactional
    public WatchlistCreateResponse register(Long userId, Long stockId) {
        stockRepository.findById(stockId)
                .orElseThrow(() -> new StockNotFoundException(stockId));

        if (watchlistRepository.existsByUserIdAndStockId(userId, stockId)) {
            throw new WatchlistAlreadyExistsException(userId, stockId);
        }

        Watchlist saved;
        try {
            saved = watchlistRepository.save(new Watchlist(userId, stockId));
        } catch (DataIntegrityViolationException e) {
            throw new WatchlistAlreadyExistsException(userId, stockId);
        }

        stockRepository.incrementWatchCount(stockId);

        return new WatchlistCreateResponse(
                saved.getId(),
                saved.getStockId(),
                saved.getUserId(),
                saved.getCreatedAt()
        );
    }

    @Transactional
    public WatchlistDeleteResponse unwatch(Long userId, Long stockId) {
        Watchlist watchlist = watchlistRepository.findByUserIdAndStockId(userId, stockId)
                .orElseThrow(() -> new WatchlistNotFoundException(userId, stockId));

        watchlistRepository.deleteByUserIdAndStockId(userId, stockId);
        stockRepository.decrementWatchCount(stockId);

        return new WatchlistDeleteResponse(watchlist.getStockId(), Instant.now());
    }

    @Transactional(readOnly = true)
    public WatchlistPageResponse getMyWatchlist(Long userId, Pageable pageable) {
        Page<Watchlist> watchlistPage = watchlistRepository.findByUserId(userId, pageable);

        List<Long> stockIds = watchlistPage.getContent().stream()
                .map(Watchlist::getStockId)
                .distinct()
                .toList();

        // Watchlist.stockId는 연관관계가 아니므로 JOIN FETCH를 사용할 수 없다.
        // 대신 stockId 목록을 모아 단일 IN 쿼리로 배치 조회하여 N+1을 회피한다.
        Map<Long, Stock> stockById = stockRepository.findAllById(stockIds).stream()
                .collect(Collectors.toMap(Stock::getId, Function.identity()));

        List<WatchlistItemResponse> content = watchlistPage.getContent().stream()
                .map(w -> toItemResponse(w, stockById.get(w.getStockId())))
                .toList();

        return new WatchlistPageResponse(
                content,
                watchlistPage.getNumber(),
                watchlistPage.getSize(),
                watchlistPage.getTotalElements()
        );
    }

    private WatchlistItemResponse toItemResponse(Watchlist watchlist, Stock stock) {
        if (stock == null) {
            throw new StockNotFoundException(watchlist.getStockId());
        }

        return new WatchlistItemResponse(
                watchlist.getId(),
                stock.getId(),
                stock.getCode(),
                stock.getName(),
                stock.getWatchCount(),
                watchlist.getCreatedAt()
        );
    }
}
