package com.arok2.stockpilot.dto.request;

import com.arok2.stockpilot.domain.InvestmentPeriod;
import com.arok2.stockpilot.domain.RiskProfile;

import jakarta.validation.constraints.NotNull;

public record UpdateProfileRequest(

        @NotNull(message = "위험 성향은 필수입니다")
        RiskProfile riskProfile,

        @NotNull(message = "투자 기간은 필수입니다")
        InvestmentPeriod investmentPeriod
) {
}
