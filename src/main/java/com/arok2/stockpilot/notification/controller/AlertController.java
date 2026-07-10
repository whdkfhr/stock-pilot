package com.arok2.stockpilot.notification.controller;

import com.arok2.stockpilot.notification.dto.AlertCreateRequest;
import com.arok2.stockpilot.notification.dto.AlertResponse;
import com.arok2.stockpilot.notification.service.AlertService;
import com.arok2.stockpilot.support.AuthenticatedUser;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    /** 가격 알림 조건 등록(인증). */
    @PostMapping
    public ResponseEntity<AlertResponse> create(
            @AuthenticatedUser Long userId,
            @Valid @RequestBody AlertCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(alertService.create(userId, request));
    }

    /** 내 알림 조건 목록(인증). */
    @GetMapping
    public ResponseEntity<List<AlertResponse>> myAlerts(@AuthenticatedUser Long userId) {
        return ResponseEntity.ok(alertService.getMyAlerts(userId));
    }

    /** 알림 조건 삭제(인증, 본인 소유만). */
    @DeleteMapping("/{alertId}")
    public ResponseEntity<Void> delete(@AuthenticatedUser Long userId, @PathVariable Long alertId) {
        alertService.delete(userId, alertId);
        return ResponseEntity.noContent().build();
    }
}
