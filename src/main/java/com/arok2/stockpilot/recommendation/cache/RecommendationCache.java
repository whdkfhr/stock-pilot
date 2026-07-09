package com.arok2.stockpilot.recommendation.cache;

import com.arok2.stockpilot.recommendation.dto.RecommendationResponse;

/**
 * 사용자별 추천 결과 캐시(Redis). 추천 계산은 비싸므로 결과를 캐싱한다(Cache-Aside).
 */
public interface RecommendationCache {

    /** 캐시된 추천 결과. 없으면 null. */
    RecommendationResponse get(Long userId);

    void put(Long userId, RecommendationResponse response);
}
