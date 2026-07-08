package com.arok2.stockpilot.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

/**
 * 인증 컨텍스트(Security Context)에서 현재 로그인한 사용자의 식별자(userId)를
 * 추출하기 위한 메타 애노테이션. Path/Body를 통한 userId 조작을 허용하지 않기 위해
 * Controller 파라미터에서 이 애노테이션만을 통해 userId를 주입받는다.
 *
 * 본 프로젝트의 JwtAuthenticationFilter는 principal로 사용자 식별자(Long)를
 * 직접 설정하므로, @AuthenticationPrincipal 로 그 값을 그대로 주입받는다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@AuthenticationPrincipal
public @interface AuthenticatedUser {
}
