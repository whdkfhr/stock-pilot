package com.arok2.stockpilot.auth.service.command;

/** 로그인 유스케이스 입력. */
public record LoginCommand(String email, String password) {
}
