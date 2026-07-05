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
import org.springframework.test.context.TestPropertySource;

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
 * 생성되는지 실제 Spring 컨텍스트를 통해 검증한다.
 *
 * 본 테스트가 검증하는 정합성 보장의 실제 근거는 애플리케이션 코드(사전 검사 +
 * 예외 변환)가 아니라 users.email 컬럼에 걸린 DB 유니크 제약(User 엔티티의
 * @UniqueConstraint uk_users_email)이다. 테스트에서는 Hibernate ddl-auto(create-drop)가
 * 이 제약을 포함해 스키마를 생성하며, 운영 환경(PostgreSQL) 역시 동일한 엔티티 매핑으로
 * 같은 유니크 제약이 적용되므로, 이 테스트에서 확인하는
 * "유니크 제약 위반 → DataIntegrityViolationException → DuplicateEmailException 변환"
 * 경로는 운영 환경에서도 동일하게 유효하다.
 *
 * 병렬 실행이 실제로 보장되도록 HikariCP 커넥션 풀의 최대 크기를
 * THREAD_COUNT 이상으로 명시적으로 설정한다(spring.datasource.hikari.maximum-pool-size).
 * 이를 통해 스레드가 커넥션 획득 대기로 순차화되어 경쟁 조건이 은폐되는 것을 방지한다.
 *
 * 제약사항: 테스트 DB는 H2(PostgreSQL 호환 모드)이며, 유니크 제약의 존재와 위반 시
 * 예외 변환 경로는 DB 종류에 무관하게 동일하게 동작하지만, 트랜잭션 격리 수준의
 * 세부 구현(예: 잠금 대기 시간, MVCC 스냅숏 처리)은 PostgreSQL과 완전히 동일하지
 * 않을 수 있다. 완전한 운영 동등성 검증을 위해서는 Testcontainers 기반 PostgreSQL
 * 테스트로의 전환을 후속 과제로 남긴다.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.hikari.maximum-pool-size=20",
        "spring.datasource.hikari.minimum-idle=20"
})
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
