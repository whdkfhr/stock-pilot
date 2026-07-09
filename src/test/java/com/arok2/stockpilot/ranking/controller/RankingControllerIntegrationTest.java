package com.arok2.stockpilot.ranking.controller;

import com.arok2.stockpilot.ranking.RankingService;
import com.arok2.stockpilot.ranking.dto.RankingItem;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 인기 랭킹 API 통합 테스트. Redis 기반 RankingService는 목으로 대체하고,
 * 공개 접근(조회수 기록·랭킹 조회)과 응답 형식을 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RankingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RankingService rankingService;

    @Test
    void 조회수_기록은_공개이며_204를_반환한다() throws Exception {
        mockMvc.perform(post("/api/stocks/000660/view"))
                .andExpect(status().isNoContent());

        verify(rankingService).recordView("000660");
    }

    @Test
    void 인기_랭킹_조회는_공개이며_순위_목록을_반환한다() throws Exception {
        when(rankingService.top(10)).thenReturn(List.of(
                new RankingItem(1, "000660", "SK하이닉스", 30L),
                new RankingItem(2, "005930", "삼성전자", 20L)));

        mockMvc.perform(get("/api/rankings/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rank", is(1)))
                .andExpect(jsonPath("$[0].code", is("000660")))
                .andExpect(jsonPath("$[0].viewCount", is(30)))
                .andExpect(jsonPath("$[1].code", is("005930")));
    }
}
