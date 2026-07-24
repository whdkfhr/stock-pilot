package com.arok2.stockpilot.controller;

import com.arok2.stockpilot.domain.InvestmentPeriod;
import com.arok2.stockpilot.domain.RiskProfile;
import com.arok2.stockpilot.domain.User;
import com.arok2.stockpilot.recommendation.cache.RecommendationCache;
import com.arok2.stockpilot.repository.UserRepository;
import com.arok2.stockpilot.security.JwtTokenProvider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 투자 성향 변경 API 통합 테스트. 변경 시 추천 캐시가 무효화되는지 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class UserProfileUpdateIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private RecommendationCache recommendationCache;

    private Long userId;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        User user = userRepository.save(User.create(
                "prof@example.com", "hash", "프로필", RiskProfile.STABLE, InvestmentPeriod.LONG_TERM));
        userId = user.getId();
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    private String bearer() {
        return "Bearer " + jwtTokenProvider.createAccessToken(userId, "prof@example.com");
    }

    @Test
    void 투자성향을_변경하면_반영되고_추천캐시를_무효화한다() throws Exception {
        mockMvc.perform(patch("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"riskProfile\":\"AGGRESSIVE\",\"investmentPeriod\":\"SHORT_TERM\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskProfile", is("AGGRESSIVE")))
                .andExpect(jsonPath("$.investmentPeriod", is("SHORT_TERM")));

        assertThat(userRepository.findById(userId).orElseThrow().getRiskProfile())
                .isEqualTo(RiskProfile.AGGRESSIVE);
        verify(recommendationCache).evict(userId);
    }

    @Test
    void 잘못된_성향값이면_400() throws Exception {
        mockMvc.perform(patch("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"riskProfile\":\"UNKNOWN\",\"investmentPeriod\":\"SHORT_TERM\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 토큰_없이_변경하면_401() throws Exception {
        mockMvc.perform(patch("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"riskProfile\":\"AGGRESSIVE\",\"investmentPeriod\":\"SHORT_TERM\"}"))
                .andExpect(status().isUnauthorized());
    }
}
