package com.arok2.stockpilot.controller;

import com.arok2.stockpilot.dto.WatchlistCreateResponse;
import com.arok2.stockpilot.dto.WatchlistDeleteResponse;
import com.arok2.stockpilot.service.WatchlistService;
import com.arok2.stockpilot.support.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stocks/{stockId}/watch")
public class WatchlistController {

    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @PostMapping
    public ResponseEntity<WatchlistCreateResponse> register(
            @PathVariable Long stockId,
            @AuthenticatedUser Long userId
    ) {
        WatchlistCreateResponse response = watchlistService.register(userId, stockId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping
    public ResponseEntity<WatchlistDeleteResponse> unwatch(
            @PathVariable Long stockId,
            @AuthenticatedUser Long userId
    ) {
        WatchlistDeleteResponse response = watchlistService.unwatch(userId, stockId);
        return ResponseEntity.ok(response);
    }
}
