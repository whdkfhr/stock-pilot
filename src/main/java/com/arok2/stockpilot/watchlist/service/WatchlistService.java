package com.arok2.stockpilot.watchlist.service;

import com.arok2.stockpilot.exception.StockNotFoundException;
import com.arok2.stockpilot.exception.WatchlistAlreadyExistsException;
import com.arok2.stockpilot.exception.WatchlistNotFoundException;
import com.arok2.stockpilot.stock.domain.Stock;
import com.arok2.stockpilot.stock.repository.StockRepository;
import com.arok2.stockpilot.watchlist.domain.Watchlist;
import com.arok2.stockpilot.watchlist.repository.WatchlistRepository;
import com.arok2.stockpilot.watchlist.service.result.WatchedStock;

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

/**
 * 관심종목 유스케이스. 도메인/결과 모델을 반환하고 API 표현은 Controller가 담당한다.
 */
@Service
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final StockRepository stockRepository;

    public WatchlistService(WatchlistRepository watchlistRepository, StockRepository stockRepository) {
        this.watchlistRepository = watchlistRepository;
        this.stockRepository = stockRepository;
    }

    @Transactional
    public Watchlist register(Long userId, Long stockId) {
        stockRepository.findById(stockId)
                .orElseThrow(() -> new StockNotFoundException(stockId));

        if (watchlistRepository.existsByUserIdAndStockId(userId, stockId)) {
            throw new WatchlistAlreadyExistsException(userId, stockId);
        }

        Watchlist saved;
        try {
            saved = watchlistRepository.save(Watchlist.register(userId, stockId));
        } catch (DataIntegrityViolationException e) {
            throw new WatchlistAlreadyExistsException(userId, stockId);
        }

        stockRepository.incrementWatchCount(stockId);

        return saved;
    }

    /** 관심종목을 해제하고 해제 시각을 돌려준다. */
    @Transactional
    public Instant unwatch(Long userId, Long stockId) {
        int deleted = watchlistRepository.deleteByUserIdAndStockId(userId, stockId);
        if (deleted == 0) {
            throw new WatchlistNotFoundException(userId, stockId);
        }
        if (deleted != 1 || stockRepository.decrementWatchCount(stockId) != 1) {
            // 카운트와 등록 내역이 불일치하면 삭제도 함께 롤백한다.
            throw new IllegalStateException("관심종목 카운트 불일치: " + stockId);
        }

        return Instant.now();
    }

    @Transactional(readOnly = true)
    public Page<WatchedStock> getMyWatchlist(Long userId, Pageable pageable) {
        Page<Watchlist> watchlistPage = watchlistRepository.findByUserId(userId, pageable);

        List<Long> stockIds = watchlistPage.getContent().stream()
                .map(Watchlist::getStockId)
                .distinct()
                .toList();

        // Watchlist.stockId는 연관관계가 아니므로 JOIN FETCH를 사용할 수 없다.
        // 대신 stockId 목록을 모아 단일 IN 쿼리로 배치 조회하여 N+1을 회피한다.
        Map<Long, Stock> stockById = stockRepository.findAllById(stockIds).stream()
                .collect(Collectors.toMap(Stock::getId, Function.identity()));

        return watchlistPage.map(w -> toWatchedStock(w, stockById.get(w.getStockId())));
    }

    private WatchedStock toWatchedStock(Watchlist watchlist, Stock stock) {
        if (stock == null) {
            throw new StockNotFoundException(watchlist.getStockId());
        }

        return new WatchedStock(
                watchlist.getId(),
                stock.getId(),
                stock.getCode(),
                stock.getName(),
                stock.getWatchCount(),
                watchlist.getCreatedAt()
        );
    }
}
