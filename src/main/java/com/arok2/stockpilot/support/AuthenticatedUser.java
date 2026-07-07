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
 * 실제 인증 주체(Principal) 구현체가 프로젝트 내 어디에 있는지에 따라
 * expression을 조정할 수 있도록 여지를 둔다. 기존 인증 체계에서 Principal이
 * userId(Long)를 직접 반환하는 커스텀 UserDetails/Principal 구현을 사용한다고 가정한다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@AuthenticationPrincipal(expression = "userId")
public @interface AuthenticatedUser {
}
