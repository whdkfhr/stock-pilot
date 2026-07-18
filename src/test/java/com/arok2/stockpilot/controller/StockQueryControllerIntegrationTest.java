package com.arok2.stockpilot.controller;

import com.arok2.stockpilot.domain.Stock;
import com.arok2.stockpilot.price.cache.LatestPriceCache;
import com.arok2.stockpilot.repository.StockRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 종목 목록 API 통합 테스트. 최신가 캐시는 목으로 대체하고, 공개 접근과 응답 형식을 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class StockQueryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StockRepository stockRepository;

    @MockitoBean
    private LatestPriceCache latestPriceCache;

    @BeforeEach
    void setUp() {
        stockRepository.deleteAll();
        stockRepository.save(Stock.of("005930", "삼성전자", 12, 1.4, 15, 2.0));
        stockRepository.save(Stock.of("000660", "SK하이닉스", 13, 1.5, 22, 1.0));
    }

    @AfterEach
    void tearDown() {
        stockRepository.deleteAll();
    }

    @Test
    void 종목_목록을_현재가와_함께_공개로_반환한다() throws Exception {
        when(latestPriceCache.get("005930")).thenReturn(61000L);
        when(latestPriceCache.get("000660")).thenReturn(null); // 아직 수집 전

        mockMvc.perform(get("/api/stocks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.code=='005930')].price", is(java.util.List.of(61000))))
                .andExpect(jsonPath("$[?(@.code=='005930')].name", is(java.util.List.of("삼성전자"))));
    }

    @Test
    void 현재가가_없으면_price는_null이다() throws Exception {
        when(latestPriceCache.get(anyString())).thenReturn(null);

        mockMvc.perform(get("/api/stocks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].code").exists());
    }

    @Test
    void q로_이름_검색하면_해당_종목만_반환한다() throws Exception {
        when(latestPriceCache.get(anyString())).thenReturn(null);

        mockMvc.perform(get("/api/stocks").param("q", "삼성"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].code", is("005930")));
    }

    @Test
    void 종목_상세를_투자지표와_시장통화와_함께_공개로_반환한다() throws Exception {
        when(latestPriceCache.get("005930")).thenReturn(61000L);

        mockMvc.perform(get("/api/stocks/{code}", "005930"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("005930")))
                .andExpect(jsonPath("$.name", is("삼성전자")))
                .andExpect(jsonPath("$.market", is("KOSPI")))
                .andExpect(jsonPath("$.currency", is("KRW")))
                .andExpect(jsonPath("$.price", is(61000)))
                .andExpect(jsonPath("$.per", is(12.0)))
                .andExpect(jsonPath("$.roe", is(15.0)));
    }

    @Test
    void 없는_종목_상세는_404() throws Exception {
        mockMvc.perform(get("/api/stocks/{code}", "999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("STOCK_NOT_FOUND")));
    }
}
