package com.arok2.stockpilot.auth.service;

import com.arok2.stockpilot.auth.service.command.LoginCommand;
import com.arok2.stockpilot.auth.service.command.SignupCommand;
import com.arok2.stockpilot.auth.service.result.IssuedToken;
import com.arok2.stockpilot.exception.DuplicateEmailException;
import com.arok2.stockpilot.exception.InvalidCredentialsException;
import com.arok2.stockpilot.security.JwtTokenProvider;
import com.arok2.stockpilot.user.domain.User;
import com.arok2.stockpilot.user.repository.UserRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 유스케이스. 입력은 Command, 출력은 도메인/결과 모델로 다루며
 * API DTO 변환은 표현 계층(Controller)이 담당한다.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public User signup(SignupCommand command) {
        if (userRepository.existsByEmail(command.email())) {
            throw new DuplicateEmailException(command.email());
        }

        String passwordHash = passwordEncoder.encode(command.password());

        User user = User.create(
                command.email(),
                passwordHash,
                command.nickname(),
                command.riskProfile(),
                command.investmentPeriod()
        );

        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            // 동시 요청으로 인한 유니크 제약 위반을 최종 방어선으로 처리
            throw new DuplicateEmailException(command.email());
        }
    }

    @Transactional(readOnly = true)
    public IssuedToken login(LoginCommand command) {
        User user = userRepository.findByEmail(command.email())
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        String token = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
        return new IssuedToken(token, jwtTokenProvider.getValiditySeconds());
    }
}
