package com.arok2.stockpilot.notification.controller;

import com.arok2.stockpilot.notification.domain.AlertCondition;
import com.arok2.stockpilot.notification.domain.AlertDirection;
import com.arok2.stockpilot.notification.repository.AlertConditionRepository;
import com.arok2.stockpilot.security.JwtTokenProvider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 알림 조건 API 통합 테스트. 실 H2 저장소를 사용하고 인증은 실제 JWT로 수행한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AlertControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AlertConditionRepository alertConditionRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private static final long ME = 100L;
    private static final long OTHER = 200L;

    @BeforeEach
    void setUp() {
        alertConditionRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        alertConditionRepository.deleteAll();
    }

    private String bearer(long id) {
        return "Bearer " + jwtTokenProvider.createAccessToken(id, "alert@example.com");
    }

    @Test
    void 알림_조건을_등록하면_201과_ACTIVE_상태를_반환한다() throws Exception {
        mockMvc.perform(post("/api/alerts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ME))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stockCode\":\"005930\",\"direction\":\"ABOVE\",\"threshold\":60000}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stockCode", is("005930")))
                .andExpect(jsonPath("$.direction", is("ABOVE")))
                .andExpect(jsonPath("$.threshold", is(60000)))
                .andExpect(jsonPath("$.status", is("ACTIVE")));

        assertThat(alertConditionRepository.findByUserIdOrderByCreatedAtDesc(ME)).hasSize(1);
    }

    @Test
    void 내_알림_조건만_목록에_조회된다() throws Exception {
        alertConditionRepository.save(AlertCondition.of(ME, "005930", AlertDirection.ABOVE, 60000));
        alertConditionRepository.save(AlertCondition.of(OTHER, "000660", AlertDirection.BELOW, 100000));

        mockMvc.perform(get("/api/alerts").header(HttpHeaders.AUTHORIZATION, bearer(ME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].stockCode", is("005930")));
    }

    @Test
    void 본인_소유_알림_조건은_삭제된다() throws Exception {
        AlertCondition saved = alertConditionRepository.save(
                AlertCondition.of(ME, "005930", AlertDirection.ABOVE, 60000));

        mockMvc.perform(delete("/api/alerts/{id}", saved.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ME)))
                .andExpect(status().isNoContent());

        assertThat(alertConditionRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    void 남의_알림_조건_삭제는_404() throws Exception {
        AlertCondition saved = alertConditionRepository.save(
                AlertCondition.of(OTHER, "005930", AlertDirection.ABOVE, 60000));

        mockMvc.perform(delete("/api/alerts/{id}", saved.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ME)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("ALERT_NOT_FOUND")));
    }

    @Test
    void 임계값이_0이하면_400() throws Exception {
        mockMvc.perform(post("/api/alerts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ME))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stockCode\":\"005930\",\"direction\":\"ABOVE\",\"threshold\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 토큰_없이_알림_조건_등록하면_401() throws Exception {
        mockMvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stockCode\":\"005930\",\"direction\":\"ABOVE\",\"threshold\":60000}"))
                .andExpect(status().isUnauthorized());
    }
}
