package com.arok2.stockpilot.price.source;

import com.arok2.stockpilot.price.event.StockPriceEvent;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * KIS 실시간 체결(H0STCNT0) 프레임 파싱 검증. 실제 wire 포맷(레코드당 46필드) 기준.
 */
class KisWebSocketParseTest {

    /** 46필드 체결 레코드 생성(핵심 필드만 지정, 나머지는 0). */
    private static String record(String code, long price, long prdyVrss, long vol) {
        String[] f = new String[46];
        for (int i = 0; i < 46; i++) f[i] = "0";
        f[0] = code;
        f[2] = String.valueOf(price);
        f[4] = String.valueOf(prdyVrss);
        f[13] = String.valueOf(vol);
        return String.join("^", f);
    }

    @Test
    void 실측_단일_체결_프레임을_파싱한다() {
        // 실제 응답에서 가져온 첫 레코드(count=1로): 현재가 250500, 전일대비 -19500, 누적거래량 11701513
        String frame = "0|H0STCNT0|001|005930^115214^250500^5^-19500^-7.22^257548.62^266000^266500"
                + "^248500^250500^250000^2^11701513^3013709268750^113664^103131^-10533^60.51^7129195"
                + "^4313943^1^0.37^73.08^090025^5^-15500^090025^5^-16000^114825^2^2000^20260724^20^N"
                + "^8930^67892^101158^466092^0.20^8331745^140.44^0^^266000";

        Optional<StockPriceEvent> e = KisWebSocketClient.parseFrame(frame);

        assertThat(e).isPresent();
        assertThat(e.get().code()).isEqualTo("005930");
        assertThat(e.get().price()).isEqualTo(250500);
        assertThat(e.get().volume()).isEqualTo(11701513);
        assertThat(e.get().previousClose()).isEqualTo(270000); // 250500 - (-19500)
    }

    @Test
    void 다건_프레임은_가장_최근_체결을_쓴다() {
        String frame = "0|H0STCNT0|002|"
                + record("005930", 250500, -19500, 100) + "^"
                + record("005930", 250000, -20000, 200);

        Optional<StockPriceEvent> e = KisWebSocketClient.parseFrame(frame);

        assertThat(e).isPresent();
        assertThat(e.get().price()).isEqualTo(250000); // 마지막 레코드
        assertThat(e.get().volume()).isEqualTo(200);
        assertThat(e.get().previousClose()).isEqualTo(270000);
    }

    @Test
    void 제어메시지와_형식불일치는_무시한다() {
        assertThat(KisWebSocketClient.parseFrame("{\"header\":{\"tr_id\":\"PINGPONG\"}}")).isEmpty();
        assertThat(KisWebSocketClient.parseFrame("{\"body\":{\"msg1\":\"SUBSCRIBE SUCCESS\"}}")).isEmpty();
        assertThat(KisWebSocketClient.parseFrame("0|H0STASP0|001|005930^abc")).isEmpty();
        assertThat(KisWebSocketClient.parseFrame("")).isEmpty();
    }
}
