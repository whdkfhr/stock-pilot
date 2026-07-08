package com.arok2.stockpilot.price.controller;

import com.arok2.stockpilot.price.cache.LatestPriceCache;
import com.arok2.stockpilot.price.domain.PriceHistory;
import com.arok2.stockpilot.price.event.StockPriceEvent;
import com.arok2.stockpilot.price.repository.PriceHistoryRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 시세 조회 API 통합 테스트. 최신가 캐시(Redis)는 목으로 대체하고, 이력은 H2로 검증한다.
 * 시세 조회는 공개(permitAll)이므로 토큰 없이 접근 가능해야 한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PriceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LatestPriceCache latestPriceCache;

    @Autowired
    private PriceHistoryRepository priceHistoryRepository;

    @AfterEach
    void tearDown() {
        priceHistoryRepository.deleteAll();
    }

    @Test
    void 최신가_조회_시_캐시값을_토큰없이_반환한다() throws Exception {
        given(latestPriceCache.get("005930")).willReturn(57000L);

        mockMvc.perform(get("/api/stocks/005930/price"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("005930")))
                .andExpect(jsonPath("$.price", is(57000)));
    }

    @Test
    void 아직_수집되지_않은_종목은_404() throws Exception {
        given(latestPriceCache.get("999999")).willReturn(null);

        mockMvc.perform(get("/api/stocks/999999/price"))
                .andExpect(status().isNotFound());
    }

    @Test
    void 이력_조회_시_저장된_시세이력을_반환한다() throws Exception {
        priceHistoryRepository.save(PriceHistory.from(
                new StockPriceEvent("005930", 100, 10, Instant.now())));
        priceHistoryRepository.save(PriceHistory.from(
                new StockPriceEvent("005930", 200, 20, Instant.now().plusSeconds(1))));

        mockMvc.perform(get("/api/stocks/005930/price/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2)));
    }
}
