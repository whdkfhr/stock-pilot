package com.arok2.stockpilot.controller;

import com.arok2.stockpilot.domain.Stock;
import com.arok2.stockpilot.repository.StockRepository;
import com.arok2.stockpilot.repository.WatchlistRepository;
import com.arok2.stockpilot.security.JwtTokenProvider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WatchlistController E2E 통합 테스트.
 * 인증은 본 프로젝트의 실제 방식(JWT Bearer 토큰)으로 수행한다 — JwtTokenProvider로
 * 특정 userId의 토큰을 발급해 Authorization 헤더로 전달하면, JwtAuthenticationFilter가
 * principal(Long userId)을 설정하고 컨트롤러의 @AuthenticatedUser 로 주입된다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class WatchlistControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private WatchlistRepository watchlistRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Long stockId;

    private String bearer(long userId) {
        return "Bearer " + jwtTokenProvider.createAccessToken(userId, "user" + userId + "@example.com");
    }

    @BeforeEach
    void setUp() {
        watchlistRepository.deleteAll();
        stockRepository.deleteAll();
        stockId = stockRepository.save(new Stock("005930", "삼성전자")).getId();
    }

    @AfterEach
    void tearDown() {
        watchlistRepository.deleteAll();
        stockRepository.deleteAll();
    }

    @Test
    void register_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(post("/api/stocks/{stockId}/watch", stockId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unwatch_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(delete("/api/stocks/{stockId}/watch", stockId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMyWatchlist_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/api/me/watchlist"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_thenUnwatch_endToEndFlow() throws Exception {
        // 등록: 201, watch_count 1 증가
        mockMvc.perform(post("/api/stocks/{stockId}/watch", stockId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(7)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stockId", is(stockId.intValue())));
        assertEquals(1L, stockRepository.findById(stockId).orElseThrow().getWatchCount());

        // 중복 등록: 409
        mockMvc.perform(post("/api/stocks/{stockId}/watch", stockId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(7)))
                .andExpect(status().isConflict());

        // 내 목록 조회: 본인이 등록한 종목만
        mockMvc.perform(get("/api/me/watchlist")
                        .header(HttpHeaders.AUTHORIZATION, bearer(7)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(1)))
                .andExpect(jsonPath("$.content[0].stockId", is(stockId.intValue())));

        // 해제: 200, watch_count 1 감소
        mockMvc.perform(delete("/api/stocks/{stockId}/watch", stockId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(7)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockId", is(stockId.intValue())));
        assertEquals(0L, stockRepository.findById(stockId).orElseThrow().getWatchCount());

        // 재해제: 404
        mockMvc.perform(delete("/api/stocks/{stockId}/watch", stockId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(7)))
                .andExpect(status().isNotFound());
    }

    @Test
    void register_withNonExistentStock_returns404() throws Exception {
        mockMvc.perform(post("/api/stocks/{stockId}/watch", 999999L)
                        .header(HttpHeaders.AUTHORIZATION, bearer(999)))
                .andExpect(status().isNotFound());
    }
}
