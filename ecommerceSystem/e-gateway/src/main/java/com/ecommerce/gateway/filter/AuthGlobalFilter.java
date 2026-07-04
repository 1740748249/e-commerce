package com.ecommerce.gateway.filter;

import cn.hutool.jwt.JWT;
import com.ecommerce.common.cache.CacheService;
import com.ecommerce.common.utils.CollUtils;
import com.ecommerce.gateway.config.AuthProperties;
import com.ecommerce.gateway.utils.JwtTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthGlobalFilter implements org.springframework.cloud.gateway.filter.GlobalFilter {
    private final AuthProperties authProperties;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();
    private final JwtTool jwtTool;
    private final CacheService cacheService;

    private static final String SHOP_MAPPING_KEY = "user:shop:mapping";

    private static class CachedToken {
        final Long userId;
        final Integer role;
        final long expiresAt;
        CachedToken(Long userId, Integer role, long expiresAt) {
            this.userId = userId;
            this.role = role;
            this.expiresAt = expiresAt;
        }
    }
    private final Map<String, CachedToken> tokenCache = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        if (isExcludePath(path)) {
            if (isSafeMethod(request) || path.contains("login") || path.contains("register")
                    || path.startsWith("/payment/")) {
                return chain.filter(exchange);
            }
        }
        List<String> auths = request.getHeaders().get("Authorization");
        if (CollUtils.isEmpty(auths) || auths.get(0).trim().isEmpty()) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
        String token = auths.get(0);
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        try {
            CachedToken ct = parseTokenCached(token);

            // 角色校验
            if (authProperties.getRolePaths() != null) {
                for (Map.Entry<String, List<Integer>> entry : authProperties.getRolePaths().entrySet()) {
                    if (antPathMatcher.match(entry.getKey(), path)) {
                        if (ct.role == null || !entry.getValue().contains(ct.role)) {
                            ServerHttpResponse response = exchange.getResponse();
                            response.setStatusCode(HttpStatus.FORBIDDEN);
                            return response.setComplete();
                        }
                        break;
                    }
                }
            }

            ServerHttpRequest mutated = exchange.getRequest().mutate()
                    .header("user-info", ct.userId.toString())
                    .build();
            if (ct.role != null) {
                mutated = mutated.mutate()
                        .header("X-User-Role", ct.role.toString())
                        .build();
            }
            Optional<Long> shopIdOpt = cacheService.hGet(SHOP_MAPPING_KEY, ct.userId.toString(), Long.class);
            if (shopIdOpt.isPresent()) {
                mutated = mutated.mutate()
                        .header("X-Shop-Id", shopIdOpt.get().toString())
                        .build();
            }
            return chain.filter(exchange.mutate().request(mutated).build());
        } catch (Exception e) {
            log.error("校验失败", e);
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
    }

    private CachedToken parseTokenCached(String token) {
        long now = System.currentTimeMillis();
        CachedToken cached = tokenCache.get(token);
        if (cached != null && now < cached.expiresAt) {
            return cached;
        }
        tokenCache.remove(token);

        Long userId = jwtTool.parseToken(token);
        Integer role = null;

        long tokenExpiry = now + 300_000;
        try {
            JWT jwt = JWT.of(token);
            Object exp = jwt.getPayload("exp");
            if (exp != null) {
                tokenExpiry = Long.parseLong(exp.toString()) * 1000L;
            }
            Object rolePayload = jwt.getPayload("role");
            if (rolePayload != null) {
                role = Integer.valueOf(rolePayload.toString());
            }
        } catch (Exception ignored) {
        }

        long cacheExpiry = Math.min(tokenExpiry, now + 300_000);
        if (cacheExpiry > now) {
            tokenCache.put(token, new CachedToken(userId, role, cacheExpiry));
        }
        return new CachedToken(userId, role, 0);
    }

    private boolean isExcludePath(String path) {
        for (String excludePath : authProperties.getExcludePaths()) {
            log.debug("尝试匹配: {} 和 {}", excludePath, path);
            if (antPathMatcher.match(excludePath, path)) {
                log.debug("匹配成功: {} = {}", excludePath, path);
                return true;
            }
        }
        log.debug("无匹配，路径 {} 需要认证", path);
        return false;
    }

    private boolean isSafeMethod(ServerHttpRequest request) {
        HttpMethod method = request.getMethod();
        return method == HttpMethod.GET || method == HttpMethod.OPTIONS;
    }
}
