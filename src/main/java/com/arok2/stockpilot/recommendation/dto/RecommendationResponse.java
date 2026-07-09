package com.arok2.stockpilot.recommendation.dto;

import java.time.Instant;
import java.util.List;

public record RecommendationResponse(
        Long userId,
        String riskProfile,
        Instant generatedAt,
        List<RecommendationItem> items
) {
}
