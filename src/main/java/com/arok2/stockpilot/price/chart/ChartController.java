package com.arok2.stockpilot.price.chart;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stocks")
public class ChartController {

    private final ChartService chartService;

    public ChartController(ChartService chartService) {
        this.chartService = chartService;
    }

    /** 기간별 차트(공개). period=1D(기본)|1W|1M */
    @GetMapping("/{code}/chart")
    public ResponseEntity<ChartResponse> chart(
            @PathVariable String code,
            @RequestParam(name = "period", defaultValue = "1D") String period) {
        return ResponseEntity.ok(chartService.getChart(code, period));
    }
}
