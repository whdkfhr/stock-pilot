package com.arok2.stockpilot.ranking.controller;

import com.arok2.stockpilot.ranking.RankingService;
import com.arok2.stockpilot.ranking.dto.RankingItem;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RankingController {

    private final RankingService rankingService;

    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    /** 종목 조회 발생 기록(공개). Redis ZINCRBY만 수행 — DB를 건드리지 않는다. */
    @PostMapping("/api/stocks/{code}/view")
    public ResponseEntity<Void> recordView(@PathVariable String code) {
        rankingService.recordView(code);
        return ResponseEntity.noContent().build();
    }

    /** 인기 종목 랭킹 TOP-N(공개). */
    @GetMapping("/api/rankings/popular")
    public ResponseEntity<List<RankingItem>> popular(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(rankingService.top(limit));
    }
}
