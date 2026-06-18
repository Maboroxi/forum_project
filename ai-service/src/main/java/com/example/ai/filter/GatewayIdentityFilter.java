package com.example.ai.filter;

import com.example.common.constants.GatewayHeaders;
import com.example.common.constants.RequestAttributes;
import com.example.common.entity.RestBean;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class GatewayIdentityFilter extends OncePerRequestFilter {

    private final String internalToken;

    public GatewayIdentityFilter(@Value("${internal.service.token}") String internalToken) {
        this.internalToken = internalToken;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/actuator/")
                || request.getRequestURI().startsWith("/error")) {
            filterChain.doFilter(request, response);
            return;
        }

        String suppliedToken = request.getHeader(GatewayHeaders.INTERNAL_TOKEN);
        String userId = request.getHeader(GatewayHeaders.USER_ID);
        if (!internalToken.equals(suppliedToken) || userId == null || userId.isBlank()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(RestBean.unauthorized("请求必须通过 Gateway 访问").asJsonString());
            return;
        }

        try {
            request.setAttribute(RequestAttributes.USER_ID, Integer.parseInt(userId));
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(RestBean.unauthorized("无效的用户身份").asJsonString());
            return;
        }
        filterChain.doFilter(request, response);
    }
}
