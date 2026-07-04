package com.arok2.stockpilot.service;

import com.arok2.stockpilot.domain.InvestmentPeriod;
import com.arok2.stockpilot.domain.RiskProfile;
import com.arok2.stockpilot.dto.request.SignupRequest;
import com.arok2.stockpilot.exception.DuplicateEmailException;
import com.arok2.stockpilot.repository.UserRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 동일 이메일로 동시에 다수의 가입 요청이 발생해도 최종적으로 하나의 계정만
 * 생성되는지 실제 Spring 컨텍스트(및 실제 DB 유니크 제약)를 통해 검증한다.
 *
 * 환경 제약(Testcontainers 미사용)으로 인해 테스트 프로파일의 실제 관계형 DB(H2)에
 * 대해 진짜 스레드 병렬 요청을 수행하여 유니크 제약 위반 시 DuplicateEmailException으로
 * 정확히 변환되는지, 그리고 최종 저장된 레코드가 정확히 1건인지를 검증한다.
 * 운영 환경(PostgreSQL)에서도 동일한 유니크 제약 기반 동작이 보장됨을 전제로 한다.
 */
@SpringBootTest
class AuthServiceConcurrencyIntegrationTest {

    private static final int THREAD_COUNT = 10;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    void 동일_이메일로_동시에_가입_요청해도_하나의_계정만_생성된다() throws Exception {
        String email = "concurrent-signup@example.com";

        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch readyLatch = new CountDownLatch(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger duplicateCount = new AtomicInteger(0);

        List<Callable<Void>> tasks = IntStream.range(0, THREAD_COUNT)
                .mapToObj(i -> (Callable<Void>) () -> {
                    readyLatch.countDown();
                    startLatch.await();
                    try {
                        authService.signup(new SignupRequest(
                                email,
                                "password123",
                                "nickname" + i,
                                RiskProfile.AGGRESSIVE,
                                InvestmentPeriod.SHORT_TERM
                        ));
                        successCount.incrementAndGet();
                    } catch (DuplicateEmailException ex) {
                        duplicateCount.incrementAndGet();
                    }
                    return null;
                })
                .toList();

        List<Future<Void>> futures = tasks.stream()
                .map(executorService::submit)
                .toList();

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();

        for (Future<Void> future : futures) {
            future.get(10, TimeUnit.SECONDS);
        }
        executorService.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(duplicateCount.get()).isEqualTo(THREAD_COUNT - 1);
        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(userRepository.existsByEmail(email)).isTrue();
    }
}
