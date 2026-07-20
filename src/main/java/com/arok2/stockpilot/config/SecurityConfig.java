package com.arok2.stockpilot.config;

import com.arok2.stockpilot.security.JwtAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 인증/인가 설정.
 * 회원가입·로그인 등 공개 엔드포인트와 actuator는 인증 없이 허용하고, 그 외 요청은
 * JWT 인증을 요구한다. JWT 기반 무상태(stateless) API이므로 세션·CSRF는 사용하지 않는다.
 * 인증 실패 시 401(Unauthorized)을 반환한다.
 */
@Configuration
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
                        // 종목 목록·상세·시세 조회는 공개 데이터
                        .requestMatchers(HttpMethod.GET, "/api/stocks", "/api/stocks/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/stocks/*/price", "/api/stocks/*/price/history").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/stocks/*/chart", "/api/stocks/*/trading-trend").permitAll()
                        // 좋아요 수 조회·인기 랭킹 조회·조회수 기록은 공개
                        .requestMatchers(HttpMethod.GET, "/api/stocks/*/likes", "/api/rankings/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/stocks/*/view").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        (request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
