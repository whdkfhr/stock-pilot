package com.arok2.stockpilot.user.service;

import com.arok2.stockpilot.exception.UserNotFoundException;
import com.arok2.stockpilot.recommendation.cache.RecommendationCache;
import com.arok2.stockpilot.user.domain.User;
import com.arok2.stockpilot.user.repository.UserRepository;
import com.arok2.stockpilot.user.service.command.UpdateProfileCommand;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 유스케이스. 도메인 객체를 반환하고 API 표현(MeResponse)은 Controller가 담당한다.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final RecommendationCache recommendationCache;

    public UserService(UserRepository userRepository, RecommendationCache recommendationCache) {
        this.userRepository = userRepository;
        this.recommendationCache = recommendationCache;
    }

    @Transactional(readOnly = true)
    public User getMe(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
    }

    /** 투자 성향·기간 변경. 추천이 성향에 의존하므로 추천 캐시를 무효화한다. */
    @Transactional
    public User updateProfile(UpdateProfileCommand command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(UserNotFoundException::new);
        user.updateProfile(command.riskProfile(), command.investmentPeriod());
        recommendationCache.evict(command.userId());
        return user;
    }
}
