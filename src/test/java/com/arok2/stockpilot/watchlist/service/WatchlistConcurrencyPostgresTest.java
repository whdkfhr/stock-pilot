package com.arok2.stockpilot.watchlist.service;

import com.arok2.stockpilot.stock.domain.Stock;
import com.arok2.stockpilot.stock.repository.StockRepository;
import com.arok2.stockpilot.support.PostgresIntegrationTest;
import com.arok2.stockpilot.watchlist.repository.WatchlistRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 관심종목 동시 등록을 <b>운영과 동일한 PostgreSQL</b>에서 검증한다.
 *
 * <p>watch_count 갱신 손실 방지는 {@code UPDATE stock SET watch_count = watch_count + 1}이
 * DB에서 row-level로 직렬화된다는 전제에 기대는데, 이 전제는 DB 엔진마다 다르다.
 * H2(PostgreSQL 호환 모드)에서 통과하는 것만으로는 운영 근거가 되지 못하므로 실제
 * PostgreSQL에서 다시 검증한다.
 *
 * <p>Docker가 없으면 자동으로 건너뛴다({@link PostgresIntegrationTest}).
 */
@SpringBootTest
@ActiveProfiles("test")
class WatchlistConcurrencyPostgresTest extends PostgresIntegrationTest {

    private static final int THREAD_COUNT = 50;

    @Autowired
    private WatchlistService watchlistService;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private WatchlistRepository watchlistRepository;

    private Long stockId;

    @BeforeEach
    void setUp() {
        watchlistRepository.deleteAll();
        stockRepository.deleteAll();
        stockId = stockRepository.save(new Stock("005930", "삼성전자")).getId();
    }

    @AfterEach
    void tearDown() {
        watchlistRepository.deleteAll();
        stockRepository.deleteAll();
    }

    @Test
    @DisplayName("실제 PostgreSQL에서 N명이 동시에 관심등록해도 watch_count는 정확히 N이다 (갱신 손실 0)")
    void 동시_관심등록_시_갱신_손실이_없다() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch ready = new CountDownLatch(THREAD_COUNT);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREAD_COUNT);
        AtomicInteger success = new AtomicInteger();

        for (long userId = 1; userId <= THREAD_COUNT; userId++) {
            long uid = userId;
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await(); // 모든 스레드를 같은 순간에 출발시켜 경합을 만든다
                    watchlistService.register(uid, stockId);
                    success.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        start.countDown();
        boolean completed = done.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).as("모든 등록이 시간 내 완료").isTrue();
        assertThat(success.get()).as("서로 다른 사용자의 등록은 모두 성공").isEqualTo(THREAD_COUNT);

        Stock updated = stockRepository.findById(stockId).orElseThrow();
        assertThat(updated.getWatchCount())
                .as("동시 등록 %d건이어도 watch_count는 정확히 %d (갱신 손실 없음)", THREAD_COUNT, THREAD_COUNT)
                .isEqualTo(THREAD_COUNT);
        assertThat(watchlistRepository.count()).isEqualTo(THREAD_COUNT);
    }

    @Test
    @DisplayName("같은 사용자가 동시에 중복 등록하면 유니크 제약으로 1건만 성공한다")
    void 동일_사용자의_중복_등록은_한_건만_성공한다() throws InterruptedException {
        int attempts = 20;
        long userId = 777L;

        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(attempts);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        for (int i = 0; i < attempts; i++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    watchlistService.register(userId, stockId);
                    success.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (RuntimeException e) {
                    rejected.incrementAndGet(); // 사전 검사 또는 유니크 제약 위반
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        start.countDown();
        assertThat(done.await(60, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        assertThat(success.get()).as("중복 등록은 1건만 성공").isEqualTo(1);
        assertThat(rejected.get()).isEqualTo(attempts - 1);
        assertThat(watchlistRepository.count()).as("row도 1건").isEqualTo(1);

        Stock updated = stockRepository.findById(stockId).orElseThrow();
        assertThat(updated.getWatchCount())
                .as("실패한 등록이 watch_count를 올려서는 안 된다")
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("등록과 해제가 동시에 일어나도 watch_count가 음수가 되거나 어긋나지 않는다")
    void 등록과_해제가_뒤섞여도_카운트가_일치한다() throws InterruptedException {
        int users = 30;
        // 먼저 절반을 등록해 두고, 이후 등록/해제를 동시에 실행한다.
        for (long uid = 1; uid <= users / 2; uid++) {
            watchlistService.register(uid, stockId);
        }

        ExecutorService executor = Executors.newFixedThreadPool(users);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(users);

        for (long uid = 1; uid <= users; uid++) {
            long id = uid;
            boolean unwatch = id <= users / 2; // 기존 등록자는 해제, 나머지는 신규 등록
            executor.submit(() -> {
                try {
                    start.await();
                    if (unwatch) {
                        watchlistService.unwatch(id, stockId);
                    } else {
                        watchlistService.register(id, stockId);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (RuntimeException ignored) {
                    // 경합으로 인한 실패는 카운트 정합성 검증 대상이 아니다
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertThat(done.await(60, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        Stock updated = stockRepository.findById(stockId).orElseThrow();
        long actualRows = watchlistRepository.count();

        assertThat(updated.getWatchCount()).as("watch_count는 음수가 될 수 없다").isNotNegative();
        assertThat(updated.getWatchCount())
                .as("watch_count는 실제 관심등록 row 수와 일치해야 한다")
                .isEqualTo(actualRows);
        assertThat(List.of(updated.getWatchCount())).isNotEmpty();
    }
}
