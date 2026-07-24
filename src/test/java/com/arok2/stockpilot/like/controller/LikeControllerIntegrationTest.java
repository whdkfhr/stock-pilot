package com.arok2.stockpilot.like.controller;

import com.arok2.stockpilot.like.LikeService;
import com.arok2.stockpilot.security.JwtTokenProvider;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 좋아요 API 통합 테스트. Redis 기반 LikeService는 목으로 대체하고,
 * 인증(POST)·공개(GET) 접근 제어와 응답 형식을 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LikeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private LikeService likeService;

    private String bearer(long id) {
        return "Bearer " + jwtTokenProvider.createAccessToken(id, "like@example.com");
    }

    @Test
    void 인증된_사용자가_좋아요를_등록하면_현재_좋아요_수를_반환한다() throws Exception {
        when(likeService.like(eq(7L), eq("000660"))).thenReturn(5L);

        mockMvc.perform(post("/api/stocks/000660/like").header(HttpHeaders.AUTHORIZATION, bearer(7L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("000660")))
                .andExpect(jsonPath("$.likeCount", is(5)));
    }

    @Test
    void 토큰_없이_좋아요_등록하면_401() throws Exception {
        mockMvc.perform(post("/api/stocks/000660/like"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 좋아요_수_조회는_토큰_없이도_공개된다() throws Exception {
        when(likeService.count("000660")).thenReturn(12L);

        mockMvc.perform(get("/api/stocks/000660/likes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("000660")))
                .andExpect(jsonPath("$.likeCount", is(12)));
    }

    @Test
    void 인증된_사용자가_좋아요를_해제하면_감소된_수를_반환한다() throws Exception {
        when(likeService.unlike(eq(7L), eq("000660"))).thenReturn(4L);

        mockMvc.perform(delete("/api/stocks/000660/like").header(HttpHeaders.AUTHORIZATION, bearer(7L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount", is(4)));
    }

    @Test
    void 토큰_없이_좋아요_해제하면_401() throws Exception {
        mockMvc.perform(delete("/api/stocks/000660/like"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 내_좋아요_상태를_조회한다() throws Exception {
        when(likeService.hasLiked(eq(7L), eq("000660"))).thenReturn(true);
        when(likeService.count("000660")).thenReturn(3L);

        mockMvc.perform(get("/api/stocks/000660/like/me").header(HttpHeaders.AUTHORIZATION, bearer(7L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked", is(true)))
                .andExpect(jsonPath("$.likeCount", is(3)));
    }

    @Test
    void 토큰_없이_좋아요_상태_조회하면_401() throws Exception {
        mockMvc.perform(get("/api/stocks/000660/like/me"))
                .andExpect(status().isUnauthorized());
    }
}
