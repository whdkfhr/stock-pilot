package com.arok2.stockpilot.controller;

import com.arok2.stockpilot.domain.InvestmentPeriod;
import com.arok2.stockpilot.domain.RiskProfile;
import com.arok2.stockpilot.domain.User;
import com.arok2.stockpilot.dto.request.SignupRequest;
import com.arok2.stockpilot.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 회원가입 API의 End-to-End 시나리오(성공/검증 실패/중복 이메일)를 실제 Spring 컨텍스트와
 * MockMvc를 통해 검증한다. 테스트 프로파일의 인메모리(H2, PostgreSQL 호환 모드) DB에
 * Flyway 마이그레이션(V1__create_users_table.sql)이 실제로 적용되어 유니크 제약 및
 * 스키마가 운영 환경과 동일하게 구성됨을 전제로 한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    private SignupRequest validRequest(String email) {
        return new SignupRequest(
                email,
                "password123",
                "nickname",
                RiskProfile.STABLE,
                InvestmentPeriod.LONG_TERM
        );
    }

    @Test
    void 유효한_요청으로_가입하면_201과_사용자정보를_응답한다() throws Exception {
        SignupRequest request = validRequest("e2e-success@example.com");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.email", is("e2e-success@example.com")))
                .andExpect(jsonPath("$.nickname", is("nickname")));
    }

    @Test
    void 가입_성공후_DB에_저장된_비밀번호는_평문과_다르고_BCrypt_형식이다() throws Exception {
        String rawPassword = "password123";
        SignupRequest request = new SignupRequest(
                "e2e-password-check@example.com",
                rawPassword,
                "nickname",
                RiskProfile.STABLE,
                InvestmentPeriod.LONG_TERM
        );

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        Optional<User> saved = userRepository.findAll().stream()
                .filter(u -> u.getEmail().equals("e2e-password-check@example.com"))
                .findFirst();

        assertThat(saved).isPresent();
        String storedHash = saved.get().getPasswordHash();

        assertThat(storedHash).isNotEqualTo(rawPassword);
        assertThat(storedHash).matches("^\\$2[aby]\\$.*");
        assertThat(passwordEncoder.matches(rawPassword, storedHash)).isTrue();
    }

    @Test
    void 이메일_형식이_올바르지_않으면_400과_검증오류를_응답한다() throws Exception {
        String invalidJson = """
                {
                  "email": "invalid-email-format",
                  "password": "password123",
                  "nickname": "nickname",
                  "riskProfile": "STABLE",
                  "investmentPeriod": "LONG_TERM"
                }
                """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.details[?(@.field == 'email')]").exists());
    }

    @Test
    void 필수값이_누락되면_400과_검증오류를_응답한다() throws Exception {
        String missingFieldsJson = """
                {
                  "email": "missing-fields@example.com"
                }
                """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(missingFieldsJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }

    @Test
    void 위험성향이_허용값이_아니면_400과_상세필드정보를_응답한다() throws Exception {
        String invalidRiskProfileJson = """
                {
                  "email": "invalid-risk@example.com",
                  "password": "password123",
                  "nickname": "nickname",
                  "riskProfile": "NOT_A_VALID_VALUE",
                  "investmentPeriod": "LONG_TERM"
                }
                """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRiskProfileJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.details[?(@.field == 'riskProfile')]").exists());
    }

    @Test
    void 투자기간이_허용값이_아니면_400과_상세필드정보를_응답한다() throws Exception {
        String invalidPeriodJson = """
                {
                  "email": "invalid-period@example.com",
                  "password": "password123",
                  "nickname": "nickname",
                  "riskProfile": "STABLE",
                  "investmentPeriod": "MID_TERM"
                }
                """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPeriodJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.details[?(@.field == 'investmentPeriod')]").exists());
    }

    @Test
    void 비밀번호_길이가_범위를_벗어나면_400을_응답한다() throws Exception {
        SignupRequest request = new SignupRequest(
                "short-password@example.com",
                "short",
                "nickname",
                RiskProfile.STABLE,
                InvestmentPeriod.LONG_TERM
        );

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }

    @Test
    void 닉네임_길이가_범위를_벗어나면_400을_응답한다() throws Exception {
        SignupRequest request = new SignupRequest(
                "short-nickname@example.com",
                "password123",
                "a",
                RiskProfile.STABLE,
                InvestmentPeriod.LONG_TERM
        );

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }

    @Test
    void 이미_존재하는_이메일로_가입하면_409를_응답한다() throws Exception {
        SignupRequest request = validRequest("duplicate@example.com");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("DUPLICATE_EMAIL")));
    }
}
