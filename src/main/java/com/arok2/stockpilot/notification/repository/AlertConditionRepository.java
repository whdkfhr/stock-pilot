package com.arok2.stockpilot.notification.repository;

import com.arok2.stockpilot.notification.domain.AlertCondition;
import com.arok2.stockpilot.notification.domain.AlertStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AlertConditionRepository extends JpaRepository<AlertCondition, Long> {

    List<AlertCondition> findByStockCodeAndStatus(String stockCode, AlertStatus status);

    List<AlertCondition> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<AlertCondition> findByIdAndUserId(Long id, Long userId);

    /**
     * 조건을 원자적으로 ACTIVE → TRIGGERED로 전이시킨다.
     * 동시 이벤트가 같은 조건을 평가하더라도 UPDATE가 성공하는 것은 한 번뿐이므로(영향 행 1),
     * 알림이 중복 생성되지 않는다. 반환값이 1일 때만 알림을 적재한다.
     */
    @Modifying
    @Query("""
            update AlertCondition a
               set a.status = com.arok2.stockpilot.notification.domain.AlertStatus.TRIGGERED,
                   a.triggeredAt = :triggeredAt
             where a.id = :id
               and a.status = com.arok2.stockpilot.notification.domain.AlertStatus.ACTIVE
            """)
    int markTriggered(@Param("id") Long id, @Param("triggeredAt") Instant triggeredAt);
}
