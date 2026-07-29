package com.arok2.stockpilot.auth.service;

import com.arok2.stockpilot.auth.service.command.SignupCommand;
import com.arok2.stockpilot.exception.DuplicateEmailException;
import com.arok2.stockpilot.support.PostgresIntegrationTest;
import com.arok2.stockpilot.user.domain.InvestmentPeriod;
import com.arok2.stockpilot.user.domain.RiskProfile;
import com.arok2.stockpilot.user.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 동일 이메일 동시 가입을 <b>운영과 동일한 PostgreSQL</b>에서 검증한다.
 *
 * <p>사전 조회(existsByEmail)만으로는 경합을 막을 수 없고 최종 방어선은 DB 유니크 제약이다.
 * 제약 위반이 실제로 어떤 예외로 올라오는지는 DB/드라이버마다 다르므로 PostgreSQL로 확인한다.
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthSignupConcurrencyPostgresTest extends PostgresIntegrationTest {

    private static final int THREAD_COUNT = 30;
    private static final String EMAIL = "race@example.com";

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("같은 이메일로 동시에 가입해도 정확히 1명만 성공한다 (유니크 제약이 최종 방어선)")
    void 동일_이메일_동시가입은_한_건만_성공한다() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch ready = new CountDownLatch(THREAD_COUNT);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREAD_COUNT);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger duplicate = new AtomicInteger();

        for (int i = 0; i < THREAD_COUNT; i++) {
            int index = i;
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    authService.signup(new SignupCommand(
                            EMAIL,
                            "password123",
                            "nickname" + index,
                            RiskProfile.AGGRESSIVE,
                            InvestmentPeriod.SHORT_TERM
                    ));
                    success.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (DuplicateEmailException e) {
                    duplicate.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        start.countDown();
        assertThat(done.await(60, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        assertThat(success.get()).as("정확히 1명만 가입 성공").isEqualTo(1);
        assertThat(duplicate.get())
                .as("나머지는 모두 중복 이메일 예외로 변환되어야 한다(원시 DB 예외 누출 금지)")
                .isEqualTo(THREAD_COUNT - 1);
        assertThat(userRepository.count()).as("DB에도 1건만 저장").isEqualTo(1);
    }
}
