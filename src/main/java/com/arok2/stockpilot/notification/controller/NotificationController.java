package com.arok2.stockpilot.notification.controller;

import com.arok2.stockpilot.notification.dto.NotificationResponse;
import com.arok2.stockpilot.notification.service.NotificationService;
import com.arok2.stockpilot.support.AuthenticatedUser;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** 내 알림 목록(인증, 최신순). */
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> myNotifications(@AuthenticatedUser Long userId) {
        return ResponseEntity.ok(notificationService.getMyNotifications(userId));
    }

    /** 알림 읽음 처리(인증, 본인 소유만). */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markRead(
            @AuthenticatedUser Long userId, @PathVariable Long notificationId) {
        return ResponseEntity.ok(notificationService.markRead(userId, notificationId));
    }
}
