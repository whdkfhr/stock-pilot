package com.arok2.stockpilot.recommendation.scoring;

import com.arok2.stockpilot.domain.RiskProfile;
import com.arok2.stockpilot.domain.Stock;

import org.springframework.stereotype.Component;

/**
 * 투자 성향별 가중치로 종목의 추천 점수(0..1)를 계산한다.
 * 각 지표를 0..1로 정규화한 뒤 성향별 가중치를 곱해 합산한다.
 */
@Component
public class RecommendationScorer {

    /** 지표별 가중치 (합 1.0). */
    public record Weights(double per, double pbr, double roe, double dividend) {
    }

    public double score(RiskProfile profile, Stock stock) {
        double perScore = lowerBetter(stock.getPer(), 5.0, 30.0);            // 저PER = 저평가
        double pbrScore = lowerBetter(stock.getPbr(), 0.5, 3.0);             // 저PBR
        double roeScore = higherBetter(stock.getRoe(), 0.0, 25.0);          // 고ROE = 성장성
        double divScore = higherBetter(stock.getDividendYield(), 0.0, 6.0); // 고배당

        Weights w = weightsFor(profile);
        return w.per() * perScore + w.pbr() * pbrScore + w.roe() * roeScore + w.dividend() * divScore;
    }

    public Weights weightsFor(RiskProfile profile) {
        return switch (profile) {
            case AGGRESSIVE -> new Weights(0.30, 0.10, 0.50, 0.10); // 성장(ROE)·저평가 중심
            case STABLE     -> new Weights(0.35, 0.35, 0.20, 0.10); // 가치·안정 중심
            case DIVIDEND   -> new Weights(0.15, 0.10, 0.20, 0.55); // 배당 중심
        };
    }

    /** 값이 낮을수록 좋은 지표를 0..1로 정규화. */
    private double lowerBetter(double value, double min, double max) {
        if (value <= min) return 1.0;
        if (value >= max) return 0.0;
        return (max - value) / (max - min);
    }

    /** 값이 높을수록 좋은 지표를 0..1로 정규화. */
    private double higherBetter(double value, double min, double max) {
        if (value <= min) return 0.0;
        if (value >= max) return 1.0;
        return (value - min) / (max - min);
    }
}
