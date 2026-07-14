package com.arok2.stockpilot.ranking;

import com.arok2.stockpilot.domain.Stock;
import com.arok2.stockpilot.observability.StockPilotMetrics;
import com.arok2.stockpilot.ranking.dto.RankingItem;
import com.arok2.stockpilot.repository.StockRepository;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 인기 종목 랭킹. Redis Sorted Set(rank:popular)에 조회수를 ZINCRBY로 누적하고,
 * ZREVRANGE로 상위 N종목을 O(logN)으로 조회한다. DB 정렬 조회를 대체한다.
 */
@Service
public class RankingService {

    private static final String KEY = "rank:popular";

    private final StringRedisTemplate redisTemplate;
    private final StockRepository stockRepository;
    private final MeterRegistry meterRegistry;

    public RankingService(StringRedisTemplate redisTemplate,
                          StockRepository stockRepository,
                          MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.stockRepository = stockRepository;
        this.meterRegistry = meterRegistry;
    }

    /** 조회 발생 시 해당 종목의 랭킹 점수를 1 증가시킨다(원자적). */
    public void recordView(String code) {
        redisTemplate.opsForZSet().incrementScore(KEY, code, 1);
        meterRegistry.counter(StockPilotMetrics.RANKING_VIEW).increment();
    }

    /** 조회수 기준 상위 limit 종목. */
    public List<RankingItem> top(int limit) {
        Set<ZSetOperations.TypedTuple<String>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(KEY, 0, limit - 1);
        if (tuples == null || tuples.isEmpty()) {
            return List.of();
        }

        Map<String, String> namesByCode = stockRepository.findAll().stream()
                .collect(Collectors.toMap(Stock::getCode, Stock::getName, (a, b) -> a));

        List<RankingItem> items = new ArrayList<>();
        int rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            String code = tuple.getValue();
            long views = tuple.getScore() == null ? 0L : tuple.getScore().longValue();
            items.add(new RankingItem(rank++, code, namesByCode.getOrDefault(code, code), views));
        }
        return items;
    }
}
