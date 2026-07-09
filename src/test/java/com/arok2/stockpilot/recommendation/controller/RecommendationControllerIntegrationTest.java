package com.arok2.stockpilot.recommendation.controller;

import com.arok2.stockpilot.domain.InvestmentPeriod;
import com.arok2.stockpilot.domain.RiskProfile;
import com.arok2.stockpilot.domain.Stock;
import com.arok2.stockpilot.domain.User;
import com.arok2.stockpilot.recommendation.cache.RecommendationCache;
import com.arok2.stockpilot.repository.StockRepository;
import com.arok2.stockpilot.repository.UserRepository;
import com.arok2.stockpilot.security.JwtTokenProvider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 추천 조회 API 통합 테스트. Redis 캐시는 목으로 대체(항상 계산), 사용자·종목은 H2로 검증한다.
 * 인증은 실제 JWT Bearer 토큰으로 수행한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RecommendationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private RecommendationCache recommendationCache; // 기본 목: get()=null → 매번 계산

    private Long userId;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        stockRepository.deleteAll();
        User user = userRepository.save(User.create(
                "rec@example.com", "hash", "추천유저", RiskProfile.DIVIDEND, InvestmentPeriod.LONG_TERM));
        userId = user.getId();
        stockRepository.save(Stock.of("GROWTH", "성장주", 15, 1.5, 25, 0.5));
        stockRepository.save(Stock.of("DIV", "배당주", 15, 1.5, 5, 6.0));
        stockRepository.save(Stock.of("MID", "중립주", 20, 2.0, 12, 2.0));
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
        stockRepository.deleteAll();
    }

    private String bearer(long id) {
        return "Bearer " + jwtTokenProvider.createAccessToken(id, "rec@example.com");
    }

    @Test
    void 배당형_사용자에게_고배당주가_1순위로_추천된다() throws Exception {
        mockMvc.perform(get("/api/recommendations").header(HttpHeaders.AUTHORIZATION, bearer(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskProfile", is("DIVIDEND")))
                .andExpect(jsonPath("$.items[0].code", is("DIV")));
    }

    @Test
    void 토큰_없이_추천_조회하면_401() throws Exception {
        mockMvc.perform(get("/api/recommendations"))
                .andExpect(status().isUnauthorized());
    }
}
