package com.arok2.stockpilot.recommendation.scoring;

import com.arok2.stockpilot.domain.RiskProfile;
import com.arok2.stockpilot.domain.Stock;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationScorerTest {

    private final RecommendationScorer scorer = new RecommendationScorer();

    // 성장주: 고ROE·저배당 / 배당주: 저ROE·고배당 (PER·PBR은 동일)
    private final Stock growthStock = Stock.of("GROWTH", "성장주", 15, 1.5, 25, 0.5);
    private final Stock dividendStock = Stock.of("DIV", "배당주", 15, 1.5, 5, 6.0);

    @Test
    void 공격형은_고ROE_성장주를_더_높게_평가한다() {
        double growth = scorer.score(RiskProfile.AGGRESSIVE, growthStock);
        double dividend = scorer.score(RiskProfile.AGGRESSIVE, dividendStock);

        assertThat(growth).isGreaterThan(dividend);
    }

    @Test
    void 배당형은_고배당주를_더_높게_평가한다() {
        double growth = scorer.score(RiskProfile.DIVIDEND, growthStock);
        double dividend = scorer.score(RiskProfile.DIVIDEND, dividendStock);

        assertThat(dividend).isGreaterThan(growth);
    }

    @Test
    void 점수는_0에서_1_사이이다() {
        double score = scorer.score(RiskProfile.STABLE, Stock.of("X", "X", 10, 1.0, 15, 3.0));

        assertThat(score).isBetween(0.0, 1.0);
    }
}
