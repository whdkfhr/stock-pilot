package com.arok2.stockpilot.watchlist.controller;

import com.arok2.stockpilot.watchlist.dto.WatchlistCreateResponse;
import com.arok2.stockpilot.watchlist.dto.WatchlistDeleteResponse;
import com.arok2.stockpilot.watchlist.dto.WatchlistPageResponse;
import com.arok2.stockpilot.watchlist.domain.Watchlist;
import com.arok2.stockpilot.watchlist.service.WatchlistService;
import com.arok2.stockpilot.watchlist.service.result.WatchedStock;
import org.springframework.data.domain.Page;
import com.arok2.stockpilot.support.AuthenticatedUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
public class WatchlistController {

    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @PostMapping("/api/stocks/{stockId}/watch")
    public ResponseEntity<WatchlistCreateResponse> register(
            @PathVariable Long stockId,
            @AuthenticatedUser Long userId
    ) {
        Watchlist created = watchlistService.register(userId, stockId);
        return ResponseEntity.status(HttpStatus.CREATED).body(WatchlistCreateResponse.from(created));
    }

    @DeleteMapping("/api/stocks/{stockId}/watch")
    public ResponseEntity<WatchlistDeleteResponse> unwatch(
            @PathVariable Long stockId,
            @AuthenticatedUser Long userId
    ) {
        Instant unwatchedAt = watchlistService.unwatch(userId, stockId);
        return ResponseEntity.ok(new WatchlistDeleteResponse(stockId, unwatchedAt));
    }

    @GetMapping("/api/me/watchlist")
    public ResponseEntity<WatchlistPageResponse> getMyWatchlist(
            @AuthenticatedUser Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<WatchedStock> watched = watchlistService.getMyWatchlist(userId, pageable);
        return ResponseEntity.ok(WatchlistPageResponse.from(watched));
    }
}
