package com.arok2.stockpilot.auth.service.command;

import com.arok2.stockpilot.user.domain.InvestmentPeriod;
import com.arok2.stockpilot.user.domain.RiskProfile;

/**
 * 회원가입 유스케이스 입력. HTTP 계약(SignupRequest)과 분리해 애플리케이션 서비스가
 * 특정 전달 매체(REST)에 묶이지 않도록 한다.
 */
public record SignupCommand(
        String email,
        String password,
        String nickname,
        RiskProfile riskProfile,
        InvestmentPeriod investmentPeriod
) {
}
