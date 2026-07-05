package com.arok2.stockpilot.service;

import com.arok2.stockpilot.domain.InvestmentPeriod;
import com.arok2.stockpilot.domain.RiskProfile;
import com.arok2.stockpilot.domain.User;
import com.arok2.stockpilot.dto.request.SignupRequest;
import com.arok2.stockpilot.dto.response.SignupResponse;
import com.arok2.stockpilot.repository.UserRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Acceptance Criteria "저장된 비밀번호 값을 조회해도 원문 비밀번호를 알아낼 수 없다"를
 * mock이 아닌 실제 PasswordEncoder(BCryptPasswordEncoder) 및 실제 DB 저장/조회 경로로 검증한다.
 *
 * AuthServiceTest(단위 테스트)는 PasswordEncoder를 mock 처리하여 흐름만 검증했으므로,
 * 본 테스트는 실제 Spring 컨텍스트를 통해 다음을 확인한다:
 *  1. 실제 BCrypt 해시가 생성되어 저장된다.
 *  2. 저장된 해시값은 평문과 다르다.
 *  3. 저장된 해시값은 BCrypt 포맷($2a$/$2b$/$2y$ prefix)을 따른다.
 *  4. 저장된 해시값으로 원본 비밀번호를 matches() 검증할 수 있다(복호화가 아닌 단방향 검증).
 *  5. DB에서 직접 조회한 엔티티의 passwordHash 역시 동일하게 원문과 다르다.
 */
@SpringBootTest
class AuthServicePasswordEncodingIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    void 실제_BCrypt_인코더로_비밀번호가_해시되어_저장되고_원문과_다르다() {
        // given
        String rawPassword = "password123";
        SignupRequest request = new SignupRequest(
                "bcrypt-check@example.com",
                rawPassword,
                "nickname",
                RiskProfile.STABLE,
                InvestmentPeriod.LONG_TERM
        );

        // when
        SignupResponse response = authService.signup(request);

        // then: DB에서 직접 조회하여 검증
        Optional<User> saved = userRepository.findById(response.id());
        assertThat(saved).isPresent();

        String storedHash = saved.get().getPasswordHash();

        assertThat(storedHash).isNotEqualTo(rawPassword);
        assertThat(storedHash).matches("^\\$2[aby]\\$.*");
        assertThat(passwordEncoder.matches(rawPassword, storedHash)).isTrue();
        assertThat(passwordEncoder.matches("wrong-password", storedHash)).isFalse();
    }
}
