package com.arok2.stockpilot.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 운영과 동일한 PostgreSQL(Testcontainers)에서 도는 통합 테스트의 베이스.
 *
 * <p>동시성처럼 <b>DB 엔진의 락·격리 동작에 결과가 좌우되는</b> 검증은 H2로는 근거가 약하다.
 * 이런 테스트만 실제 PostgreSQL로 올리고, 나머지는 빠른 H2 프로파일을 유지한다.
 *
 * <p>{@code disabledWithoutDocker = true}이므로 Docker가 없는 환경에서는 자동으로 건너뛴다.
 * 덕분에 "인프라 없이도 {@code ./gradlew test}가 통과한다"는 규칙이 유지된다.
 */
@Testcontainers(disabledWithoutDocker = true)
public abstract class PostgresIntegrationTest {

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", PostgresTestContainer.INSTANCE::getJdbcUrl);
        registry.add("spring.datasource.username", PostgresTestContainer.INSTANCE::getUsername);
        registry.add("spring.datasource.password", PostgresTestContainer.INSTANCE::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        // 테스트 프로파일이 H2Dialect로 고정돼 있으므로 여기서 되돌린다.
        registry.add("spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }
}
