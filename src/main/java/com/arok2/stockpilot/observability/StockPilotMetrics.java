package com.arok2.stockpilot.observability;

/**
 * 도메인 비즈니스 메트릭 이름 상수. 아키텍처 선택(캐시·이벤트·동시성)의 효과를
 * 수치로 관측하기 위한 커스텀 Micrometer 메트릭을 한 곳에서 관리한다.
 */
public final class StockPilotMetrics {

    private StockPilotMetrics() {
    }

    /** 추천 Cache-Aside 조회 수. tag result=hit|miss 로 캐시 적중률을 관측한다. */
    public static final String RECOMMENDATION_CACHE = "stockpilot.recommendation.cache";

    /** 추천 점수 계산(캐시 미스 시) 소요 시간. 캐시 대비 계산 비용을 관측한다. */
    public static final String RECOMMENDATION_COMPUTE = "stockpilot.recommendation.compute";

    /** 가격 알림 발화 수(조건 충족 → 알림 생성). */
    public static final String ALERT_TRIGGERED = "stockpilot.alert.triggered";

    /** 좋아요 등록 요청 수. */
    public static final String LIKE_REGISTERED = "stockpilot.like.registered";

    /** 종목 조회수 기록(랭킹 ZINCRBY) 수. */
    public static final String RANKING_VIEW = "stockpilot.ranking.view";

    public static final String TAG_RESULT = "result";
    public static final String RESULT_HIT = "hit";
    public static final String RESULT_MISS = "miss";
}
