package com.arok2.stockpilot.recommendation.service;

import com.arok2.stockpilot.user.domain.RiskProfile;
import com.arok2.stockpilot.user.domain.User;
import com.arok2.stockpilot.exception.UserNotFoundException;
import com.arok2.stockpilot.recommendation.cache.RecommendationCache;
import com.arok2.stockpilot.recommendation.service.result.Recommendation;
import com.arok2.stockpilot.recommendation.service.result.ScoredStock;
import com.arok2.stockpilot.observability.StockPilotMetrics;
import com.arok2.stockpilot.recommendation.scoring.RecommendationScorer;
import com.arok2.stockpilot.stock.repository.StockRepository;
import com.arok2.stockpilot.user.repository.UserRepository;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
public class RecommendationService {

    private static final int TOP_N = 5;

    private final UserRepository userRepository;
    private final StockRepository stockRepository;
    private final RecommendationScorer scorer;
    private final RecommendationCache recommendationCache;
    private final MeterRegistry meterRegistry;

    public RecommendationService(UserRepository userRepository,
                                 StockRepository stockRepository,
                                 RecommendationScorer scorer,
                                 RecommendationCache recommendationCache,
                                 MeterRegistry meterRegistry) {
        this.userRepository = userRepository;
        this.stockRepository = stockRepository;
        this.scorer = scorer;
        this.recommendationCache = recommendationCache;
        this.meterRegistry = meterRegistry;
    }

    /**
     * 사용자 성향 기반 추천 상위 종목. Cache-Aside: Redis에 있으면 즉시 반환, 없으면 계산 후 캐싱.
     * 캐시 적중/미스와 계산 소요 시간을 메트릭으로 관측한다.
     */
    @Transactional(readOnly = true)
    public Recommendation recommend(Long userId) {
        Recommendation cached = recommendationCache.get(userId);
        if (cached != null) {
            meterRegistry.counter(StockPilotMetrics.RECOMMENDATION_CACHE,
                    StockPilotMetrics.TAG_RESULT, StockPilotMetrics.RESULT_HIT).increment();
            return cached;
        }
        meterRegistry.counter(StockPilotMetrics.RECOMMENDATION_CACHE,
                StockPilotMetrics.TAG_RESULT, StockPilotMetrics.RESULT_MISS).increment();

        return Timer.builder(StockPilotMetrics.RECOMMENDATION_COMPUTE)
                .register(meterRegistry)
                .record(() -> computeAndCache(userId));
    }

    private Recommendation computeAndCache(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        RiskProfile profile = user.getRiskProfile();

        List<ScoredStock> items = stockRepository.findAll().stream()
                .map(stock -> new ScoredStock(
                        stock.getCode(),
                        stock.getName(),
                        round(scorer.score(profile, stock))))
                .sorted(Comparator.comparingDouble(ScoredStock::score).reversed())
                .limit(TOP_N)
                .toList();

        Recommendation recommendation = new Recommendation(userId, profile, Instant.now(), items);
        recommendationCache.put(userId, recommendation);
        return recommendation;
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
