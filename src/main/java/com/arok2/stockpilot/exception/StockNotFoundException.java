package com.arok2.stockpilot.exception;

public class StockNotFoundException extends RuntimeException {

    public StockNotFoundException(Long stockId) {
        super("Stock not found: id=" + stockId);
    }

    public StockNotFoundException(String code) {
        super("Stock not found: code=" + code);
    }
}
