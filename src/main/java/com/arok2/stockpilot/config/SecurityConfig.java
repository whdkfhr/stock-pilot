package com.arok2.stockpilot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.arok2.stockpilot.exception.ErrorResponse;
import com.arok2.stockpilot.security.JwtAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 인증/인가 설정.
 * 회원가입·로그인 등 공개 엔드포인트와 actuator는 인증 없이 허용하고, Watchlist 엔드포인트를
 * 포함한 그 외 요청은 JWT 인증을 요구한다. JWT 기반 무상태(stateless) API이므로 세션·CSRF는
 * 사용하지 않는다. 인증 실패 시 401(Unauthorized)을 반환한다.
 *
 * TASK-002 Dependency("인증/인가 체계 — 선행 구현 완료 상태로 가정")에 따라 기존
 * {@link JwtAuthenticationFilter}(토큰 파싱 및 Authentication 객체 생성)를 그대로 재사용하며,
 * 이 필터 체인 위에서 동작하는 인가 규칙과 미인증 시 응답 처리를 함께 명시한다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api/stocks/**/watch", "/api/me/watchlist").authenticated()
                        .anyRequest().authenticated())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(unauthorizedEntryPoint())
                        .accessDeniedHandler(new BearerTokenAccessDeniedHandler())
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 인증되지 않은 요청이 보호된 엔드포인트에 접근할 경우, 필터 단계에서 곧바로
     * 401 JSON 응답을 내려준다. Spring Security는 필터 체인에서 예외를 가로채므로
     * @RestControllerAdvice까지 전파되지 않는 경우가 일반적이며, 이 EntryPoint가
     * 401 보장의 실질적인 최종 지점이다.
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
