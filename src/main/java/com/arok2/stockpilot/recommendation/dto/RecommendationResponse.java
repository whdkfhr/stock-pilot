package com.arok2.stockpilot.recommendation.dto;

import com.arok2.stockpilot.recommendation.service.result.Recommendation;

import java.time.Instant;
import java.util.List;

public record RecommendationResponse(
        Long userId,
        String riskProfile,
        Instant generatedAt,
        List<RecommendationItem> items
) {

    /** 추천 결과를 API 표현으로 변환한다(성향 enum → 문자열). */
    public static RecommendationResponse from(Recommendation recommendation) {
        return new RecommendationResponse(
                recommendation.userId(),
                recommendation.riskProfile().name(),
                recommendation.generatedAt(),
                recommendation.items().stream().map(RecommendationItem::from).toList()
        );
    }
}
