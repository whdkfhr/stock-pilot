package com.arok2.stockpilot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @Scheduled 기반 시세 수집 스케줄러를 활성화한다.
 * 실제 스케줄러(PriceCollectorScheduler)는 stockpilot.price.collector.enabled 로 on/off 된다.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
