package com.arok2.stockpilot.service;

import com.arok2.stockpilot.domain.User;
import com.arok2.stockpilot.dto.request.SignupRequest;
import com.arok2.stockpilot.dto.response.SignupResponse;
import com.arok2.stockpilot.exception.DuplicateEmailException;
import com.arok2.stockpilot.repository.UserRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }

        String passwordHash = passwordEncoder.encode(request.password());

        User user = User.create(
                request.email(),
                passwordHash,
                request.nickname(),
                request.riskProfile(),
                request.investmentPeriod()
        );

        try {
            User saved = userRepository.saveAndFlush(user);
            return SignupResponse.from(saved);
        } catch (DataIntegrityViolationException ex) {
            // 동시 요청으로 인한 유니크 제약 위반을 최종 방어선으로 처리
            throw new DuplicateEmailException(request.email());
        }
    }
}
