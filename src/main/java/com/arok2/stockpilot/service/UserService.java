package com.arok2.stockpilot.service;

import com.arok2.stockpilot.domain.User;
import com.arok2.stockpilot.dto.response.MeResponse;
import com.arok2.stockpilot.exception.UserNotFoundException;
import com.arok2.stockpilot.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public MeResponse getMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        return MeResponse.from(user);
    }
}
