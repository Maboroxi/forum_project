package com.example.config;

import com.example.common.entity.RestBean;
import com.example.filter.GatewayIdentityFilter;
import com.example.utils.Const;
import jakarta.annotation.Resource;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.DispatcherTypeRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Security configuration for the monolith.
 * Login/logout/JWT creation have been moved to user-service.
 * Gateway injects identity headers; GatewayIdentityFilter restores them.
 */
@Configuration
public class SecurityConfiguration {

    @Resource
    GatewayIdentityFilter gatewayIdentityFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(conf -> conf
                        .requestMatchers("/api/auth/**", "/error").permitAll()
                        .requestMatchers("/api/user/**").permitAll()
                        .requestMatchers("/internal/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole(Const.ROLE_ADMIN)
                        .anyRequest().hasAnyRole(Const.ROLE_DEFAULT, Const.ROLE_ADMIN)
                )
                .exceptionHandling(conf -> conf
                        .accessDeniedHandler(this::handleProcess)
                        .authenticationEntryPoint(this::handleProcess)
                )
                .securityMatcher(new DispatcherTypeRequestMatcher(DispatcherType.REQUEST))
                .securityMatcher(new NegatedRequestMatcher(new DispatcherTypeRequestMatcher(DispatcherType.ASYNC)))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(conf -> conf
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(gatewayIdentityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    private void handleProcess(HttpServletRequest request,
                               HttpServletResponse response,
                               Object exceptionOrAuthentication) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        PrintWriter writer = response.getWriter();
        if (exceptionOrAuthentication instanceof AccessDeniedException exception) {
            writer.write(RestBean
                    .forbidden(exception.getMessage()).asJsonString());
        } else if (exceptionOrAuthentication instanceof Exception exception) {
            writer.write(RestBean
                    .unauthorized(exception.getMessage()).asJsonString());
        }
    }
}
