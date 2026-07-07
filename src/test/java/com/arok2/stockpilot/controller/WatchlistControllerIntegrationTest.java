package com.arok2.stockpilot.controller;

import com.arok2.stockpilot.domain.Stock;
import com.arok2.stockpilot.domain.Watchlist;
import com.arok2.stockpilot.repository.StockRepository;
import com.arok2.stockpilot.repository.WatchlistRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * WatchlistController에 대한 End-to-End 통합 테스트.
 * 실제 Spring 컨텍스트(SecurityFilterChain 포함)와 실제 DB(테스트 프로파일 데이터소스)를
 * 통해 등록/해제/조회 및 인증 실패 시나리오를 검증한다.
 *
 * 인증 주체(userId) 주입은 프로젝트 기존 인증 체계의 테스트 지원 방식(@WithMockUser 커스터마이징
 * 또는 동등한 테스트 시큐리티 설정)을 통해 이루어진다고 가정한다. userId=7 사용자로 인증된 상태를
 * 가정하는 커스텀 애노테이션/설정이 기존에 없다면, 아래 @WithUserId 를 프로젝트의 실제 테스트
 * 인증 지원 메커니즘으로 교체해야 한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WatchlistControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private WatchlistRepository watchlistRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Long stockId;

    @BeforeEach
    void setUp() {
        watchlistRepository.deleteAll();
        stockRepository.deleteAll();
        Stock stock = stockRepository.save(new Stock("005930", "삼성전자"));
        stockId = stock.getId();
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
    @Transactional
    @WithMockUser(username = "7")
    void register_thenUnwatch_endToEndFlow() throws Exception {
        // 등록: 201 및 watch_count 1 증가 확인
        mockMvc.perform(post("/api/stocks/{stockId}/watch", stockId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stockId", is(stockId.intValue())));

        Stock afterRegister = stockRepository.findById(stockId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(1L, afterRegister.getWatchCount());

        // 중복 등록: 409
        mockMvc.perform(post("/api/stocks/{stockId}/watch", stockId))
                .andExpect(status().isConflict());

        // 내 목록 조회: 본인이 등록한 종목만 반환
        mockMvc.perform(get("/api/me/watchlist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(1)))
                .andExpect(jsonPath("$.content[0].stockId", is(stockId.intValue())));

        // 해제: 200 및 watch_count 1 감소 확인
        mockMvc.perform(delete("/api/stocks/{stockId}/watch", stockId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockId", is(stockId.intValue())));

        Stock afterUnwatch = stockRepository.findById(stockId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(0L, afterUnwatch.getWatchCount());

        // 해제 후 재해제: 404
        mockMvc.perform(delete("/api/stocks/{stockId}/watch", stockId))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    @WithMockUser(username = "999")
    void register_withNonExistentStock_returns404() throws Exception {
        mockMvc.perform(post("/api/stocks/{stockId}/watch", 999999L))
                .andExpect(status().isNotFound());
    }
}
