package com.arok2.stockpilot.notification.controller;

import com.arok2.stockpilot.notification.domain.Notification;
import com.arok2.stockpilot.notification.repository.NotificationRepository;
import com.arok2.stockpilot.security.JwtTokenProvider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 알림 조회/읽음 API 통합 테스트. 실 H2 저장소 + 실제 JWT 인증.
 */
@SpringBootTest
@AutoConfigureMockMvc
class NotificationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private static final long ME = 100L;
    private static final long OTHER = 200L;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        notificationRepository.deleteAll();
    }

    private String bearer(long id) {
        return "Bearer " + jwtTokenProvider.createAccessToken(id, "noti@example.com");
    }

    @Test
    void 내_알림만_조회된다() throws Exception {
        notificationRepository.save(Notification.of(ME, "005930", "삼성전자 알림", 61000));
        notificationRepository.save(Notification.of(OTHER, "000660", "SK 알림", 130000));

        mockMvc.perform(get("/api/notifications").header(HttpHeaders.AUTHORIZATION, bearer(ME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].stockCode", is("005930")))
                .andExpect(jsonPath("$[0].read", is(false)));
    }

    @Test
    void 알림_읽음_처리하면_read가_true가_된다() throws Exception {
        Notification saved = notificationRepository.save(Notification.of(ME, "005930", "삼성전자 알림", 61000));

        mockMvc.perform(patch("/api/notifications/{id}/read", saved.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read", is(true)));

        assertThat(notificationRepository.findById(saved.getId()).orElseThrow().isRead()).isTrue();
    }

    @Test
    void 남의_알림_읽음_처리는_404() throws Exception {
        Notification saved = notificationRepository.save(Notification.of(OTHER, "005930", "알림", 61000));

        mockMvc.perform(patch("/api/notifications/{id}/read", saved.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ME)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOTIFICATION_NOT_FOUND")));
    }

    @Test
    void 토큰_없이_알림_조회하면_401() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
    }
}
