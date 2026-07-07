package com.arok2.stockpilot.controller;

import com.arok2.stockpilot.dto.WatchlistCreateResponse;
import com.arok2.stockpilot.dto.WatchlistDeleteResponse;
import com.arok2.stockpilot.dto.WatchlistPageResponse;
import com.arok2.stockpilot.service.WatchlistService;
import com.arok2.stockpilot.support.AuthenticatedUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        WatchlistCreateResponse response = watchlistService.register(userId, stockId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/api/stocks/{stockId}/watch")
    public ResponseEntity<WatchlistDeleteResponse> unwatch(
            @PathVariable Long stockId,
            @AuthenticatedUser Long userId
    ) {
        WatchlistDeleteResponse response = watchlistService.unwatch(userId, stockId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/me/watchlist")
    public ResponseEntity<WatchlistPageResponse> getMyWatchlist(
            @AuthenticatedUser Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        WatchlistPageResponse response = watchlistService.getMyWatchlist(userId, pageable);
        return ResponseEntity.ok(response);
    }
}
