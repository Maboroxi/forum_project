package com.example.user.filter;

import com.example.common.constants.GatewayHeaders;
import com.example.user.utils.Const;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

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
        String userId = request.getHeader(GatewayHeaders.USER_ID);
        String suppliedToken = request.getHeader(GatewayHeaders.INTERNAL_TOKEN);
        if (internalToken.equals(suppliedToken) && userId != null && !userId.isBlank()) {
            request.setAttribute(Const.ATTR_USER_ID, Integer.parseInt(userId));
            String username = request.getHeader(GatewayHeaders.USERNAME);
            String roles = request.getHeader(GatewayHeaders.USER_ROLES);
            User user = (User) User.withUsername(username == null || username.isBlank() ? "gateway-user" : username)
                    .password("******")
                    .authorities(parseAuthorities(roles))
                    .build();
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }

    private List<SimpleGrantedAuthority> parseAuthorities(String roles) {
        if (roles == null || roles.isBlank()) {
            return List.of(new SimpleGrantedAuthority("ROLE_user"));
        }
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .map(SimpleGrantedAuthority::new)
                .toList();
    }
}
