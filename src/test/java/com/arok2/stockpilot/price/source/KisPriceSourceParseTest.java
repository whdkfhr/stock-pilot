package com.arok2.stockpilot.price.source;

import com.arok2.stockpilot.price.event.StockPriceEvent;
import com.arok2.stockpilot.repository.StockRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * KIS 국내 현재가 응답 파싱 검증(네트워크 없이).
 */
class KisPriceSourceParseTest {

    private KisPriceSource newSource() {
        return new KisPriceSource(
                new KisProperties(),
                Mockito.mock(KisTokenClient.class),
                new ObjectMapper(),
                Mockito.mock(StockRepository.class),
                Mockito.mock(YahooPriceFetcher.class));
    }

    @Test
    void 상승_응답에서_현재가_거래량_전일종가를_추출한다() {
        String json = """
                {"rt_cd":"0","msg1":"정상","output":{
                  "stck_prpr":"260500","acml_vol":"22292003",
                  "prdy_vrss":"1500","prdy_vrss_sign":"2","prdy_ctrt":"0.58"}}
                """;

        StockPriceEvent e = newSource().parseDomestic(json, "005930");

        assertThat(e.price()).isEqualTo(260500);
        assertThat(e.volume()).isEqualTo(22292003);
        assertThat(e.previousClose()).isEqualTo(259000); // 260500 - (+1500)
    }

    @Test
    void 하락_응답은_전일종가가_현재가보다_높다() {
        String json = """
                {"rt_cd":"0","output":{
                  "stck_prpr":"260500","acml_vol":"1000",
                  "prdy_vrss":"2000","prdy_vrss_sign":"5"}}
                """;

        StockPriceEvent e = newSource().parseDomestic(json, "005930");

        assertThat(e.previousClose()).isEqualTo(262500); // 260500 - (-2000)
    }

    @Test
    void 오류_응답이면_예외를_던진다() {
        String json = "{\"rt_cd\":\"1\",\"msg1\":\"조회 오류\",\"output\":{}}";

        assertThatThrownBy(() -> newSource().parseDomestic(json, "005930"))
                .isInstanceOf(IllegalStateException.class);
    }
}
