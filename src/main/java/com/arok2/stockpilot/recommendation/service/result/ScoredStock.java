package com.arok2.stockpilot.recommendation.service.result;

/** 추천 점수가 매겨진 종목. */
public record ScoredStock(String code, String name, double score) {
}
