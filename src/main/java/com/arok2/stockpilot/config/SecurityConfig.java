package com.arok2.stockpilot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.arok2.stockpilot.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.http.HttpStatus;

/**
 * TASK-002의 Dependency: "인증/인가 체계(로그인한 사용자 식별) — 선행 구현 완료 상태로 가정"에 따라,
 * JWT 기반 인증 필터/토큰 검증 로직 자체는 기존 구현을 재사용한다는 전제 하에,
 * 여기서는 Watchlist 관련 엔드포인트가 인증 없이 접근될 경우 401을 반환하도록
 * 최소한의 인가 규칙과 AuthenticationEntryPoint만 명시한다.
 *
 * 인증 필터(토큰 파싱/Authentication 객체 생성)는 별도의 Security 구성 요소로
 * 이미 등록되어 있다고 가정하며, 본 설정은 그 필터 체인 위에서 동작하는
 * 인가 규칙과 미인증 시 응답 처리만을 담당한다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/stocks/**/watch", "/api/me/watchlist").authenticated()
                        .anyRequest().permitAll()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(unauthorizedEntryPoint())
                        .accessDeniedHandler(new BearerTokenAccessDeniedHandler())
                );

        return http.build();
    }

    /**
     * 인증되지 않은 요청이 Watchlist 엔드포인트에 접근할 경우, 필터 단계에서
     * 곧바로 401 JSON 응답을 내려준다. Spring Security는 필터 체인에서 예외를
     * 가로채므로 @RestControllerAdvice까지 전파되지 않는 경우가 일반적이며,
     * 이 EntryPoint가 401 보장의 실질적인 최종 지점이다.
     */
    @Bean
    public AuthenticationEntryPoint unauthorizedEntryPoint() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ErrorResponse body = ErrorResponse.of("UNAUTHORIZED", "인증이 필요합니다");
            response.getWriter().write(objectMapper.writeValueAsString(body));
        };
    }
}
