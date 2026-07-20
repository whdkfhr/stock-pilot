package com.arok2.stockpilot.price.chart;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Yahoo chart 시계열 파싱 검증(네트워크 없이).
 */
class YahooChartClientParseTest {

    private final YahooChartClient client =
            new YahooChartClient(RestClient.builder(), new ObjectMapper());

    @Test
    void 타임스탬프와_종가를_추출하고_null_구간은_건너뛴다() {
        String json = """
                {"chart":{"result":[{
                  "timestamp":[1000,2000,3000],
                  "indicators":{"quote":[{"close":[100.0,null,102.5]}]}
                }]}}
                """;

        List<ChartPoint> points = client.parse(json);

        assertThat(points).hasSize(2);
        assertThat(points.get(0).close()).isEqualTo(100.0);
        assertThat(points.get(1).close()).isEqualTo(102.5);
        assertThat(points.get(0).time().getEpochSecond()).isEqualTo(1000);
    }

    @Test
    void 결과가_없으면_빈_목록() {
        assertThat(client.parse("{\"chart\":{\"result\":[]}}")).isEmpty();
        assertThat(client.parse("not-json")).isEmpty();
    }
}
