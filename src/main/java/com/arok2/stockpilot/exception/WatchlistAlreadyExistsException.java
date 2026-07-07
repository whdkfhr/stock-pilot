package com.arok2.stockpilot.exception;

public class WatchlistAlreadyExistsException extends RuntimeException {

    public WatchlistAlreadyExistsException(Long userId, Long stockId) {
        super("Watchlist already exists: userId=" + userId + ", stockId=" + stockId);
    }
}
