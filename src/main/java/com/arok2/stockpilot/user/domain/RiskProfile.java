package com.arok2.stockpilot.user.domain;

/**
 * 투자 위험 성향. 성향별 추천 지표 가중치는 성향 자체의 속성이므로 여기서 관리한다
 * (계산기 쪽 switch 분기 제거 — 성향이 추가되면 이 enum만 확장하면 된다).
 */
public enum RiskProfile {

    /** 성장(ROE)·저평가 중심. */
    AGGRESSIVE(0.30, 0.10, 0.50, 0.10),

    /** 가치·안정 중심. */
    STABLE(0.35, 0.35, 0.20, 0.10),

    /** 배당 중심. */
    DIVIDEND(0.15, 0.10, 0.20, 0.55);

    /** 지표별 가중치 (합 1.0). */
    public record Weights(double per, double pbr, double roe, double dividend) {
    }

    private final Weights weights;

    RiskProfile(double per, double pbr, double roe, double dividend) {
        this.weights = new Weights(per, pbr, roe, dividend);
    }

    public Weights weights() {
        return weights;
    }
}
