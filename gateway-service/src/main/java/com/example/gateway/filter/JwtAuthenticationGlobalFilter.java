package com.example.gateway.filter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.common.constants.GatewayHeaders;
import com.example.common.constants.RedisKeys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Component
public class JwtAuthenticationGlobalFilter implements GlobalFilter, Ordered {

    private static final String JWT_BLACK_LIST = RedisKeys.JWT_BLACK_LIST;
    private static final String BANNED_BLOCK = RedisKeys.BANNED_BLOCK;

    private static final Set<String> PUBLIC_PREFIXES = Set.of(
            "/api/auth/",
            "/images/",
            "/actuator/",
            "/error"
    );

    private final ReactiveStringRedisTemplate redisTemplate;
    private final JWTVerifier verifier;
    private final String internalToken;

    public JwtAuthenticationGlobalFilter(ReactiveStringRedisTemplate redisTemplate,
                                         @Value("${spring.security.jwt.key}") String jwtKey,
                                         @Value("${internal.service.token}") String internalToken) {
        this.redisTemplate = redisTemplate;
        this.verifier = JWT.require(Algorithm.HMAC256(jwtKey)).build();
        this.internalToken = internalToken;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().pathWithinApplication().value();

        if (isPublicRequest(request, path)) {
            return chain.filter(removeIdentityHeaders(exchange));
        }

        DecodedJWT jwt = resolveJwt(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        if (jwt == null) {
            return writeFailure(exchange, HttpStatus.UNAUTHORIZED, 401, "登录状态已过期，请重新登录！");
        }

        Integer userId = jwt.getClaim("id").asInt();
        String username = jwt.getClaim("name").asString();
        List<String> roles = jwt.getClaim("authorities").asList(String.class);
        if (userId == null || roles == null || roles.isEmpty()) {
            return writeFailure(exchange, HttpStatus.UNAUTHORIZED, 401, "登录状态已过期，请重新登录！");
        }

        if (path.startsWith("/api/admin/") && !roles.contains("ROLE_admin")) {
            return writeFailure(exchange, HttpStatus.FORBIDDEN, 403, "权限不足，无法访问管理接口");
        }

        return redisTemplate.hasKey(JWT_BLACK_LIST + jwt.getId())
                .flatMap(invalidToken -> Boolean.TRUE.equals(invalidToken)
                        ? writeFailure(exchange, HttpStatus.UNAUTHORIZED, 401, "登录状态已过期，请重新登录！")
                        : redisTemplate.hasKey(BANNED_BLOCK + userId)
                                .flatMap(banned -> Boolean.TRUE.equals(banned)
                                        ? writeFailure(exchange, HttpStatus.FORBIDDEN, 403, "账户已被封禁")
                                        : chain.filter(withIdentityHeaders(exchange, userId, username, roles))));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }

    private boolean isPublicRequest(ServerHttpRequest request, String path) {
        if (request.getMethod() == HttpMethod.OPTIONS) {
            return true;
        }
        return PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private DecodedJWT resolveJwt(String authorization) {
        String token = convertToken(authorization);
        if (token == null) {
            return null;
        }
        try {
            DecodedJWT jwt = verifier.verify(token);
            Date expiresAt = jwt.getExpiresAt();
            return expiresAt != null && expiresAt.before(new Date()) ? null : jwt;
        } catch (JWTVerificationException e) {
            return null;
        }
    }

    private String convertToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        String token = authorization.substring(7).trim();
        return token.isEmpty() || "undefined".equals(token) || "null".equals(token) ? null : token;
    }

    private ServerWebExchange removeIdentityHeaders(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(GatewayHeaders.USER_ID);
                    headers.remove(GatewayHeaders.USERNAME);
                    headers.remove(GatewayHeaders.USER_ROLES);
                    headers.remove(GatewayHeaders.INTERNAL_TOKEN);
                    headers.set(GatewayHeaders.INTERNAL_TOKEN, internalToken);
                })
                .build();
        return exchange.mutate().request(request).build();
    }

    private ServerWebExchange withIdentityHeaders(ServerWebExchange exchange,
                                                  int userId,
                                                  String username,
                                                  List<String> roles) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(GatewayHeaders.USER_ID);
                    headers.remove(GatewayHeaders.USERNAME);
                    headers.remove(GatewayHeaders.USER_ROLES);
                    headers.remove(GatewayHeaders.INTERNAL_TOKEN);
                    headers.set(GatewayHeaders.USER_ID, String.valueOf(userId));
                    headers.set(GatewayHeaders.USERNAME, Objects.toString(username, ""));
                    headers.set(GatewayHeaders.USER_ROLES, String.join(",", roles));
                    headers.set(GatewayHeaders.INTERNAL_TOKEN, internalToken);
                })
                .build();
        return exchange.mutate().request(request).build();
    }

    private Mono<Void> writeFailure(ServerWebExchange exchange, HttpStatus httpStatus, int code, String message) {
        exchange.getResponse().setStatusCode(httpStatus);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"id\":0,\"code\":" + code + ",\"data\":null,\"message\":\"" + message + "\"}";
        DataBuffer buffer = exchange.getResponse().bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
