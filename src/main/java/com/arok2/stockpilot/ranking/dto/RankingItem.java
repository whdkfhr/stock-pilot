package com.arok2.stockpilot.ranking.dto;

public record RankingItem(
        int rank,
        String code,
        String name,
        long viewCount
) {
}
