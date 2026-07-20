package com.arok2.stockpilot.price.chart;

import com.arok2.stockpilot.price.quote.QuoteMeta;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Yahoo chart 시계열(OHLCV)·요약 메타 파싱 검증(네트워크 없이).
 */
class YahooChartClientParseTest {

    private final YahooChartClient client =
            new YahooChartClient(RestClient.builder(), new ObjectMapper());

    @Test
    void OHLCV_봉을_추출하고_close가_null인_봉은_건너뛴다() {
        String json = """
                {"chart":{"result":[{
                  "timestamp":[1000,2000,3000],
                  "indicators":{"quote":[{
                    "open":[99.0,100.0,101.0],
                    "high":[101.0,102.0,103.0],
                    "low":[98.0,99.0,100.0],
                    "close":[100.0,null,102.5],
                    "volume":[500,600,700]
                  }]}
                }]}}
                """;

        List<ChartPoint> points = client.parse(json);

        assertThat(points).hasSize(2);
        assertThat(points.get(0).close()).isEqualTo(100.0);
        assertThat(points.get(0).high()).isEqualTo(101.0);
        assertThat(points.get(0).volume()).isEqualTo(500);
        assertThat(points.get(1).close()).isEqualTo(102.5);
        assertThat(points.get(0).time().getEpochSecond()).isEqualTo(1000);
    }

    @Test
    void 결과가_없으면_빈_목록() {
        assertThat(client.parse("{\"chart\":{\"result\":[]}}")).isEmpty();
        assertThat(client.parse("not-json")).isEmpty();
    }

    @Test
    void meta에서_고저_52주_거래량_이름을_추출한다() {
        String json = """
                {"chart":{"result":[{"meta":{
                  "regularMarketDayHigh":257500.0,"regularMarketDayLow":240000.0,
                  "regularMarketVolume":17966728,
                  "fiftyTwoWeekHigh":374500.0,"fiftyTwoWeekLow":64900.0,
                  "longName":"Samsung Electronics Co., Ltd.","exchangeName":"KSC"
                }}]}}
                """;

        QuoteMeta meta = client.parseMeta(json);

        assertThat(meta.dayHigh()).isEqualTo(257500.0);
        assertThat(meta.dayLow()).isEqualTo(240000.0);
        assertThat(meta.volume()).isEqualTo(17966728);
        assertThat(meta.fiftyTwoWeekHigh()).isEqualTo(374500.0);
        assertThat(meta.name()).contains("Samsung");
        assertThat(meta.exchange()).isEqualTo("KSC");
    }
}
