package com.arok2.stockpilot.exception;

public class WatchlistNotFoundException extends RuntimeException {

    public WatchlistNotFoundException(Long userId, Long stockId) {
        super("Watchlist not found: userId=" + userId + ", stockId=" + stockId);
    }
}
