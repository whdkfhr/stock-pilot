package com.arok2.stockpilot.recommendation.service;

import com.arok2.stockpilot.domain.InvestmentPeriod;
import com.arok2.stockpilot.domain.RiskProfile;
import com.arok2.stockpilot.domain.Stock;
import com.arok2.stockpilot.domain.User;
import com.arok2.stockpilot.recommendation.cache.RecommendationCache;
import com.arok2.stockpilot.recommendation.dto.RecommendationItem;
import com.arok2.stockpilot.recommendation.dto.RecommendationResponse;
import com.arok2.stockpilot.recommendation.scoring.RecommendationScorer;
import com.arok2.stockpilot.repository.StockRepository;
import com.arok2.stockpilot.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private RecommendationCache recommendationCache;

    private RecommendationService service;

    @BeforeEach
    void setUp() {
        service = new RecommendationService(
                userRepository, stockRepository, new RecommendationScorer(), recommendationCache);
    }

    @Test
    void 캐시미스면_계산하고_결과를_캐시에_저장한다() {
        given(recommendationCache.get(1L)).willReturn(null);
        given(userRepository.findById(1L)).willReturn(Optional.of(
                User.create("u@e.com", "hash", "닉", RiskProfile.DIVIDEND, InvestmentPeriod.LONG_TERM)));
        given(stockRepository.findAll()).willReturn(List.of(
                Stock.of("GROWTH", "성장주", 15, 1.5, 25, 0.5),
                Stock.of("DIV", "배당주", 15, 1.5, 5, 6.0)));

        RecommendationResponse response = service.recommend(1L);

        assertThat(response.riskProfile()).isEqualTo("DIVIDEND");
        // 배당형이므로 배당주가 1위
        assertThat(response.items().get(0).code()).isEqualTo("DIV");
        verify(recommendationCache).put(1L, response);
    }

    @Test
    void 캐시히트면_계산없이_캐시값을_반환한다() {
        RecommendationResponse cached = new RecommendationResponse(
                1L, "STABLE", Instant.now(), List.of(new RecommendationItem("X", "X", 0.9)));
        given(recommendationCache.get(1L)).willReturn(cached);

        RecommendationResponse response = service.recommend(1L);

        assertThat(response).isSameAs(cached);
        verifyNoInteractions(userRepository, stockRepository);
        verify(recommendationCache, never()).put(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any());
    }
}
