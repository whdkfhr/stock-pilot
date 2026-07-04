```java
package com.arok2.stockpilot.service;

import com.arok2.stockpilot.domain.InvestmentPeriod;
import com.arok2.stockpilot.domain.RiskProfile;
import com.arok2.stockpilot.domain.User;
import com.arok2.stockpilot.dto.request.SignupRequest;
import com.arok2.stockpilot.dto.response.SignupResponse;
import com.arok2.stockpilot.exception.DuplicateEmailException;
import com.arok2.stockpilot.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder);
    }

    private SignupRequest validRequest() {
        return new SignupRequest(
                "user@example.com",
                "password123",
                "nick",
                RiskProfile.STABLE,
                InvestmentPeriod.LONG_TERM
        );
    }

    @Test
    void 유효한_요청으로_가입에_성공하면_식별자_이메일_닉네임을_반환한다() {
        // given
        SignupRequest request = validRequest();
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("hashed-password");

        User savedUser = User.create(
                request.email(), "hashed-password", request.nickname(),
                request.riskProfile(), request.investmentPeriod()
        );
        ReflectionTestUtils.setField(savedUser, "id", 1L);

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // when
        SignupResponse response = authService.signup(request);

        // then
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo(request.email());
        assertThat(response.nickname()).isEqualTo(request.nickname());
    }

    @Test
    void 비밀번호는_평문이_아닌_해시값으로_저장된다() {
        // given
        SignupRequest request = validRequest();
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        authService.signup(request);

        // then
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User capturedUser = captor.getValue();

        assertThat(capturedUser.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(capturedUser.getPasswordHash()).isNotEqualTo(request.password());
    }

    @Test
    void 이미_존재하는_이메일이면_사전_검사에서_DuplicateEmailException을_던진다() {
        // given
        SignupRequest request = validRequest();
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(DuplicateEmailException.class);

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void 사전_검사를_통과했지만_저장_시_DB_제약_위반이_발생하면_DuplicateEmailException으로_변환한다() {
        // given
        SignupRequest request = validRequest();
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("hashed-password");
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint violation"));

        // when & then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(DuplicateEmailException.class);
    }
}
```
