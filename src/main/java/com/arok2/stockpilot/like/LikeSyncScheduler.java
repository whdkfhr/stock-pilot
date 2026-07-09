package com.arok2.stockpilot.like;

import com.arok2.stockpilot.repository.StockRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Redis의 좋아요 수를 주기적으로 DB(stock.like_count)에 동기화한다(Redis→DB 정합성).
 * 좋아요 클릭 자체는 DB를 건드리지 않고 Redis에서 처리하므로, 여기서 배치로만 반영한다.
 */
@Component
@ConditionalOnProperty(name = "stockpilot.like.sync.enabled", havingValue = "true", matchIfMissing = true)
public class LikeSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(LikeSyncScheduler.class);

    private final StockRepository stockRepository;
    private final LikeService likeService;

    public LikeSyncScheduler(StockRepository stockRepository, LikeService likeService) {
        this.stockRepository = stockRepository;
        this.likeService = likeService;
    }

    @Scheduled(fixedDelayString = "${stockpilot.like.sync.interval-ms:10000}")
    @Transactional
    public void sync() {
        var stocks = stockRepository.findAll();
        for (var stock : stocks) {
            long count = likeService.count(stock.getCode());
            stock.updateLikeCount(count); // 변경 감지(dirty checking)로 UPDATE
        }
        if (!stocks.isEmpty()) {
            log.debug("좋아요 {}종목 DB 동기화", stocks.size());
        }
    }
}
