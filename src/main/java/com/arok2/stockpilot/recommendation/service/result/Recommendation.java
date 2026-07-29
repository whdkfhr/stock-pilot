package com.arok2.stockpilot.recommendation.service.result;

import com.arok2.stockpilot.user.domain.RiskProfile;

import java.time.Instant;
import java.util.List;

/**
 * 성향 기반 추천 결과. 성향을 문자열이 아닌 {@link RiskProfile}로 들고 있으며,
 * API 표현 변환은 Controller가 담당한다. Redis 캐시에도 이 형태로 저장한다.
 */
public record Recommendation(
        Long userId,
        RiskProfile riskProfile,
        Instant generatedAt,
        List<ScoredStock> items
) {
}
