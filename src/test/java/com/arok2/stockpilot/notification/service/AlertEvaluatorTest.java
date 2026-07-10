package com.arok2.stockpilot.notification.service;

import com.arok2.stockpilot.domain.Stock;
import com.arok2.stockpilot.notification.domain.AlertCondition;
import com.arok2.stockpilot.notification.domain.AlertDirection;
import com.arok2.stockpilot.notification.domain.AlertStatus;
import com.arok2.stockpilot.notification.domain.Notification;
import com.arok2.stockpilot.notification.repository.AlertConditionRepository;
import com.arok2.stockpilot.notification.repository.NotificationRepository;
import com.arok2.stockpilot.price.event.StockPriceEvent;
import com.arok2.stockpilot.repository.StockRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertEvaluatorTest {

    @Mock
    private AlertConditionRepository alertConditionRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private StockRepository stockRepository;

    private StockPriceEvent event(long price) {
        return new StockPriceEvent("005930", price, 1000, Instant.now());
    }

    @Test
    void 조건을_충족하고_원자적_전이에_성공하면_알림을_생성한다() {
        AlertCondition condition = AlertCondition.of(7L, "005930", AlertDirection.ABOVE, 60_000);
        when(alertConditionRepository.findByStockCodeAndStatus("005930", AlertStatus.ACTIVE))
                .thenReturn(List.of(condition));
        when(stockRepository.findByCode("005930"))
                .thenReturn(Optional.of(Stock.of("005930", "삼성전자", 12, 1.4, 15, 2.0)));
        when(alertConditionRepository.markTriggered(any(), any(Instant.class))).thenReturn(1);

        AlertEvaluator evaluator = new AlertEvaluator(alertConditionRepository, notificationRepository, stockRepository);
        evaluator.evaluate(event(61_000));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(7L);
        assertThat(saved.getStockCode()).isEqualTo("005930");
        assertThat(saved.getPrice()).isEqualTo(61_000);
        assertThat(saved.getMessage()).contains("삼성전자").contains("60,000").contains("이상");
    }

    @Test
    void 조건을_충족하지_못하면_전이도_알림도_없다() {
        AlertCondition condition = AlertCondition.of(7L, "005930", AlertDirection.ABOVE, 60_000);
        when(alertConditionRepository.findByStockCodeAndStatus("005930", AlertStatus.ACTIVE))
                .thenReturn(List.of(condition));

        AlertEvaluator evaluator = new AlertEvaluator(alertConditionRepository, notificationRepository, stockRepository);
        evaluator.evaluate(event(59_000));

        verify(alertConditionRepository, never()).markTriggered(anyLong(), any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void 이미_다른_이벤트가_발화시켜_전이가_0행이면_알림을_생성하지_않는다() {
        AlertCondition condition = AlertCondition.of(7L, "005930", AlertDirection.ABOVE, 60_000);
        when(alertConditionRepository.findByStockCodeAndStatus("005930", AlertStatus.ACTIVE))
                .thenReturn(List.of(condition));
        when(stockRepository.findByCode("005930")).thenReturn(Optional.empty());
        when(alertConditionRepository.markTriggered(any(), any(Instant.class))).thenReturn(0);

        AlertEvaluator evaluator = new AlertEvaluator(alertConditionRepository, notificationRepository, stockRepository);
        evaluator.evaluate(event(61_000));

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void 활성_조건이_없으면_아무것도_하지_않는다() {
        when(alertConditionRepository.findByStockCodeAndStatus("005930", AlertStatus.ACTIVE))
                .thenReturn(List.of());

        AlertEvaluator evaluator = new AlertEvaluator(alertConditionRepository, notificationRepository, stockRepository);
        evaluator.evaluate(event(61_000));

        verify(stockRepository, never()).findByCode(eq("005930"));
        verify(notificationRepository, never()).save(any());
    }
}
