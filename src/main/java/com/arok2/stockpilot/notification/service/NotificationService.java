package com.arok2.stockpilot.notification.service;

import com.arok2.stockpilot.exception.NotificationNotFoundException;
import com.arok2.stockpilot.notification.domain.Notification;
import com.arok2.stockpilot.notification.dto.NotificationResponse;
import com.arok2.stockpilot.notification.repository.NotificationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 사용자의 알림 조회 및 읽음 처리.
 */
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional
    public NotificationResponse markRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
        notification.markRead();
        return NotificationResponse.from(notification);
    }
}
