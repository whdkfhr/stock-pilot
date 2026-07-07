package com.arok2.stockpilot.dto;

import java.util.List;

public record WatchlistPageResponse(
        List<WatchlistItemResponse> content,
        int page,
        int size,
        long totalElements
) {
}
