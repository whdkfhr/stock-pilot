package com.arok2.stockpilot.dto.response;

import com.arok2.stockpilot.domain.InvestmentPeriod;
import com.arok2.stockpilot.domain.RiskProfile;
import com.arok2.stockpilot.domain.User;

public record MeResponse(
        Long id,
        String email,
        String nickname,
        RiskProfile riskProfile,
        InvestmentPeriod investmentPeriod
) {
    public static MeResponse from(User user) {
        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRiskProfile(),
                user.getInvestmentPeriod()
        );
    }
}
