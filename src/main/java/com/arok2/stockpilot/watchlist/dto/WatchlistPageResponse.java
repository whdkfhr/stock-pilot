package com.arok2.stockpilot.watchlist.dto;

import com.arok2.stockpilot.watchlist.service.result.WatchedStock;
import org.springframework.data.domain.Page;

import java.util.List;

public record WatchlistPageResponse(
        List<WatchlistItemResponse> content,
        int page,
        int size,
        long totalElements
) {

    public static WatchlistPageResponse from(Page<WatchedStock> page) {
        return new WatchlistPageResponse(
                page.getContent().stream().map(WatchlistItemResponse::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements()
        );
    }
}
