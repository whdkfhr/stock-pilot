package com.arok2.stockpilot.observability;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prometheus 노출 검증: 애플리케이션이 PrometheusMeterRegistry를 구성하고,
 * 커스텀 도메인 메트릭이 스크레이프(exposition) 출력에 포함되는지 확인한다.
 * (실제 스크레이프는 /actuator/prometheus가 이 레지스트리의 scrape()를 그대로 반환한다.)
 * @SpringBootTest는 기본적으로 export 레지스트리를 끄므로 @AutoConfigureObservability로 켠다.
 */
@SpringBootTest
@AutoConfigureObservability
class MetricsEndpointIntegrationTest {

    @Autowired
    private PrometheusMeterRegistry prometheusMeterRegistry;

    @Test
    void 프로메테우스_스크레이프에_커스텀_메트릭과_공통태그가_노출된다() {
        prometheusMeterRegistry.counter(StockPilotMetrics.LIKE_REGISTERED).increment();

        String scrape = prometheusMeterRegistry.scrape();

        assertThat(scrape)
                .contains("application=\"stock-pilot\"")
                .contains("stockpilot_like_registered_total");
    }
}
