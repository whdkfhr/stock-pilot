package com.arok2.stockpilot.recommendation.scoring;

import com.arok2.stockpilot.user.domain.RiskProfile;
import com.arok2.stockpilot.stock.domain.Stock;

import org.springframework.stereotype.Component;

/**
 * 종목의 추천 점수(0..1)를 계산한다.
 * 각 지표를 0..1로 정규화한 뒤 성향별 가중치({@link RiskProfile#weights()})를 곱해 합산한다.
 */
@Component
public class RecommendationScorer {

    public double score(RiskProfile profile, Stock stock) {
        double perScore = lowerBetter(stock.getPer(), 5.0, 30.0);            // 저PER = 저평가
        double pbrScore = lowerBetter(stock.getPbr(), 0.5, 3.0);             // 저PBR
        double roeScore = higherBetter(stock.getRoe(), 0.0, 25.0);          // 고ROE = 성장성
        double divScore = higherBetter(stock.getDividendYield(), 0.0, 6.0); // 고배당

        RiskProfile.Weights w = profile.weights();
        return w.per() * perScore + w.pbr() * pbrScore + w.roe() * roeScore + w.dividend() * divScore;
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
