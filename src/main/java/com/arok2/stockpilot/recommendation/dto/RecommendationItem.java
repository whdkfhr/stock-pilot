package com.arok2.stockpilot.recommendation.dto;

import com.arok2.stockpilot.recommendation.service.result.ScoredStock;

public record RecommendationItem(
        String code,
        String name,
        double score
) {

    public static RecommendationItem from(ScoredStock scored) {
        return new RecommendationItem(scored.code(), scored.name(), scored.score());
    }
}
