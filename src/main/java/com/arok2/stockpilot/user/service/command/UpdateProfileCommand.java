package com.arok2.stockpilot.user.service.command;

import com.arok2.stockpilot.user.domain.InvestmentPeriod;
import com.arok2.stockpilot.user.domain.RiskProfile;

/** 투자 성향·기간 변경 유스케이스 입력. 대상 사용자까지 포함한 완결된 입력이다. */
public record UpdateProfileCommand(
        Long userId,
        RiskProfile riskProfile,
        InvestmentPeriod investmentPeriod
) {
}
