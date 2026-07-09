package com.arok2.stockpilot.recommendation.controller;

import com.arok2.stockpilot.recommendation.dto.RecommendationResponse;
import com.arok2.stockpilot.recommendation.service.RecommendationService;
import com.arok2.stockpilot.support.AuthenticatedUser;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로그인한 사용자의 성향 기반 추천 종목을 반환한다.
 */
@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public ResponseEntity<RecommendationResponse> recommend(@AuthenticatedUser Long userId) {
        return ResponseEntity.ok(recommendationService.recommend(userId));
    }
}
