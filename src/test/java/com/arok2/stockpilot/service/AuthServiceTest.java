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

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
                "nickname",
                RiskProfile.STABLE,
                InvestmentPeriod.LONG_TERM
        );
    }

    private User userWithId(SignupRequest request, String hash, Long id) throws Exception {
        User user = User.create(
                request.email(), hash, request.nickname(),
                request.riskProfile(), request.investmentPeriod()
        );
        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, id);
        return user;
    }

    @Test
    void 유효한_요청이면_회원가입에_성공하고_응답에_비밀번호가_포함되지_않는다() throws Exception {
        // given
        SignupRequest request = validRequest();
        given(userRepository.existsByEmail(request.email())).willReturn(false);
        given(passwordEncoder.encode(request.password())).willReturn("hashed-password");

        User savedUser = userWithId(request, "hashed-password", 1L);
        given(userRepository.saveAndFlush(any(User.class))).willReturn(savedUser);

        // when
        SignupResponse response = authService.signup(request);

        // then
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo(request.email());
        assertThat(response.nickname()).isEqualTo(request.nickname());
    }

    @Test
    void 비밀번호는_평문이_아닌_해시로_저장된다() {
        // given
        SignupRequest request = validRequest();
        given(userRepository.existsByEmail(request.email())).willReturn(false);
        given(passwordEncoder.encode(request.password())).willReturn("hashed-password");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        given(userRepository.saveAndFlush(userCaptor.capture()))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        authService.signup(request);

        // then
        User captured = userCaptor.getValue();
        assertThat(captured.getPasswordHash()).isNotEqualTo(request.password());
        assertThat(captured.getPasswordHash()).isEqualTo("hashed-password");
    }

    @Test
    void 이미_존재하는_이메일이면_사전_검사에서_예외가_발생한다() {
        // given
        SignupRequest request = validRequest();
        given(userRepository.existsByEmail(request.email())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(DuplicateEmailException.class);

        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void 동시_요청으로_유니크_제약_위반이_발생하면_중복_이메일_예외로_변환된다() {
        // given
        SignupRequest request = validRequest();
        given(userRepository.existsByEmail(request.email())).willReturn(false);
        given(passwordEncoder.encode(anyString())).willReturn("hashed-password");
        given(userRepository.saveAndFlush(any(User.class)))
                .willThrow(new DataIntegrityViolationException("unique constraint violation"));

        // when & then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(DuplicateEmailException.class);
    }
}
