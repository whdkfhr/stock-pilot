package com.arok2.stockpilot.recommendation.service;

import com.arok2.stockpilot.domain.RiskProfile;
import com.arok2.stockpilot.domain.User;
import com.arok2.stockpilot.exception.UserNotFoundException;
import com.arok2.stockpilot.recommendation.cache.RecommendationCache;
import com.arok2.stockpilot.recommendation.dto.RecommendationItem;
import com.arok2.stockpilot.recommendation.dto.RecommendationResponse;
import com.arok2.stockpilot.recommendation.scoring.RecommendationScorer;
import com.arok2.stockpilot.repository.StockRepository;
import com.arok2.stockpilot.repository.UserRepository;

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

    public RecommendationService(UserRepository userRepository,
                                 StockRepository stockRepository,
                                 RecommendationScorer scorer,
                                 RecommendationCache recommendationCache) {
        this.userRepository = userRepository;
        this.stockRepository = stockRepository;
        this.scorer = scorer;
        this.recommendationCache = recommendationCache;
    }

    /**
     * 사용자 성향 기반 추천 상위 종목. Cache-Aside: Redis에 있으면 즉시 반환, 없으면 계산 후 캐싱.
     */
    @Transactional(readOnly = true)
    public RecommendationResponse recommend(Long userId) {
        RecommendationResponse cached = recommendationCache.get(userId);
        if (cached != null) {
            return cached;
        }

        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        RiskProfile profile = user.getRiskProfile();

        List<RecommendationItem> items = stockRepository.findAll().stream()
                .map(stock -> new RecommendationItem(
                        stock.getCode(),
                        stock.getName(),
                        round(scorer.score(profile, stock))))
                .sorted(Comparator.comparingDouble(RecommendationItem::score).reversed())
                .limit(TOP_N)
                .toList();

        RecommendationResponse response = new RecommendationResponse(
                userId, profile.name(), Instant.now(), items);
        recommendationCache.put(userId, response);
        return response;
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
