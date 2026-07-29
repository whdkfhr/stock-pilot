package com.arok2.stockpilot.support;

import org.testcontainers.containers.PostgreSQLContainer;

/**
 * 테스트 전체가 공유하는 PostgreSQL 컨테이너(싱글턴).
 *
 * <p>클래스마다 컨테이너를 띄우면 기동 비용이 커지므로 최초 접근 시 한 번만 시작하고
 * JVM 종료 시 함께 정리되도록 한다(Ryuk).
 *
 * <p>이 클래스는 {@link PostgresIntegrationTest}가 Docker 가용성을 확인한 뒤에만
 * 참조되므로, Docker가 없는 환경에서는 로드되지 않는다.
 */
final class PostgresTestContainer {

    static final PostgreSQLContainer<?> INSTANCE =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("stockpilot_test")
                    .withUsername("stockpilot")
                    .withPassword("stockpilot");

    static {
        INSTANCE.start();
    }

    private PostgresTestContainer() {
    }
}
