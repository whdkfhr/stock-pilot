package com.arok2.stockpilot.recommendation.dto;

public record RecommendationItem(
        String code,
        String name,
        double score
) {
}
