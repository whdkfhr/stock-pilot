package com.arok2.stockpilot.exception;

/**
 * 이메일이 존재하지 않거나 비밀번호가 일치하지 않을 때 발생한다.
 * 보안상 어느 쪽이 틀렸는지 구분하지 않고 동일한 메시지를 사용한다.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("이메일 또는 비밀번호가 올바르지 않습니다");
    }
}
