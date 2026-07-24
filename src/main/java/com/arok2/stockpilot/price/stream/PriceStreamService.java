package com.arok2.stockpilot.price.stream;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SSE 구독자(브라우저) 레지스트리 + 시세 틱 브로드캐스트. Kafka 시세 이벤트를 소비하는
 * {@link PriceStreamConsumer}가 broadcast를 호출해 연결된 모든 클라이언트에 push한다.
 */
@Service
public class PriceStreamService {

    private static final long TIMEOUT_MS = 30 * 60 * 1000L; // 30분(만료 시 브라우저가 자동 재연결)

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        return emitter;
    }

    public void broadcast(PriceTick tick) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("price").data(tick));
            } catch (IOException | IllegalStateException e) {
                emitters.remove(emitter); // 끊긴 연결(브라우저 종료 등) 정리
            }
        }
    }

    /** 현재 연결 수(테스트/관측용). */
    public int subscriberCount() {
        return emitters.size();
    }
}
