package com.arok2.stockpilot.service;

import com.arok2.stockpilot.domain.Stock;
import com.arok2.stockpilot.repository.StockRepository;
import com.arok2.stockpilot.repository.WatchlistRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 실제 데이터 저장소(테스트 프로파일 DB)를 사용하여, 동일 종목에 대해 N명의
 * 서로 다른 사용자가 동시에 관심종목 등록을 요청했을 때 watch_count가 정확히
 * N만큼 증가하는지 검증하는 동시성 통합 테스트.
 *
 * WatchlistService.register(...)는 트랜잭션 경계를 가지는 Spring Bean 메서드이므로,
 * 스레드마다 독립적으로 이 메서드를 직접 호출하여 각기 별도의 트랜잭션/커넥션에서
 * DB 원자적 UPDATE(watch_count = watch_count + 1)가 수행되도록 한다.
 *
 * 테스트 환경 제약(Testcontainers 미사용)으로 인해 기본 테스트 프로파일의 DB
 * (H2 등)를 사용하되, 실제 UPDATE 문이 row-level에서 직렬화되는 동작을 검증하는
 * 데 초점을 둔다. PostgreSQL 등 운영 DB와 락 동작이 다를 수 있다는 한계가 있다.
 */
@SpringBootTest
@ActiveProfiles("test")
class WatchlistServiceConcurrencyIntegrationTest {

    @Autowired
    private WatchlistService watchlistService;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private WatchlistRepository watchlistRepository;

    private Long stockId;

    private static final int THREAD_COUNT = 30;

    @BeforeEach
    void setUp() {
        watchlistRepository.deleteAll();
        stockRepository.deleteAll();
        Stock stock = stockRepository.save(new Stock("005930", "삼성전자"));
        stockId = stock.getId();
    }

    @AfterEach
    void tearDown() {
        watchlistRepository.deleteAll();
        stockRepository.deleteAll();
    }

    @Test
    void concurrentRegister_bySeparateUsers_incrementsWatchCountExactlyByThreadCount() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch readyLatch = new CountDownLatch(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        List<Long> userIds = new ArrayList<>();
        for (long i = 1; i <= THREAD_COUNT; i++) {
            userIds.add(i);
        }

        for (Long userId : userIds) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    watchlistService.register(userId, stockId);
                    successCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(true, completed, "모든 등록 요청이 시간 내에 완료되어야 한다");
        assertEquals(THREAD_COUNT, successCount.get(), "서로 다른 사용자의 등록은 모두 성공해야 한다");

        Stock updated = stockRepository.findById(stockId).orElseThrow();
        assertEquals(
                THREAD_COUNT,
                updated.getWatchCount(),
                "동시에 N명이 등록해도 watch_count는 정확히 N만큼 증가해야 한다"
        );

        long watchlistRowCount = watchlistRepository.count();
        assertEquals(THREAD_COUNT, watchlistRowCount, "등록된 watchlist row 수도 N과 일치해야 한다");
    }
}
