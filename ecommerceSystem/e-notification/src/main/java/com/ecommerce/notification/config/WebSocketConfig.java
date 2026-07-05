package com.ecommerce.notification.config;

import com.ecommerce.api.client.ProductClient;
import com.ecommerce.common.cache.CacheService;
import com.ecommerce.common.domain.R;
import com.ecommerce.notification.utils.JwtTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import javax.annotation.PostConstruct;

/**
 * STOMP-over-WebSocket 配置。
 * 商家浏览器通过此连接实时接收订单通知推送。
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker  // 开启 STOMP 协议 + WebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtTool jwtTool;
    private final CacheService cacheService;
    private final ProductClient productClient;

    /** WebSocket CORS 允许的源域名模式，生产环境应限制为具体前端域名 */
    @Value("${ecommerce.websocket.allowed-origin:*}")
    private String allowedOrigin;

    @PostConstruct
    public void warnOnWildcardOrigin() {
        if ("*".equals(allowedOrigin)) {
            log.warn("WebSocket allowed-origin 为通配符 '*', 生产环境请配置 ecommerce.websocket.allowed-origin 为具体域名");
        }
    }

    // ==================== 消息代理 ====================

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 内置代理 —— 前缀 /topic 的消息直接推给订阅客户端，不经过 Controller
        registry.enableSimpleBroker("/topic");
        // 客户端主动发消息时使用的前缀（本功能前端只订阅，不发消息）
        registry.setApplicationDestinationPrefixes("/app");
    }

    // ==================== 连接端点 ====================

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 前端连接地址：new SockJS('/ws')
        // SockJS 作为降级方案：浏览器不支持 WebSocket 时自动切换为 HTTP 长轮询
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigin)
                .withSockJS();                   // 启用 SockJS 兜底
    }

    // ==================== 连接认证 ====================

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                        message, StompHeaderAccessor.class);
                if (accessor == null) return message;

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String token = accessor.getFirstNativeHeader("Authorization");
                    if (token == null || token.isBlank()) {
                        throw new IllegalArgumentException("未提供认证信息，请先登录");
                    }
                    if (token.startsWith("Bearer ")) {
                        token = token.substring(7);
                    }
                    JwtTool.TokenInfo info = jwtTool.parseJwt(token);
                    // 存入 session，SUBSCRIBE 阶段用于鉴权
                    accessor.getSessionAttributes().put("userId", info.userId);
                    accessor.getSessionAttributes().put("role", info.role);
                    log.debug("WebSocket 连接认证成功: userId={}, role={}", info.userId, info.role);
                }

                if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    String dest = accessor.getDestination();
                    if (dest != null && dest.startsWith("/topic/shop/")) {
                        Object userIdObj = accessor.getSessionAttributes().get("userId");
                        if (userIdObj == null) {
                            throw new IllegalArgumentException("未登录，请先连接认证");
                        }
                        Long userId = (Long) userIdObj;
                        // 提取目标 shopId
                        String shopIdStr = dest.substring("/topic/shop/".length());
                        long targetShopId;
                        try {
                            targetShopId = Long.parseLong(shopIdStr);
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("无效的店铺ID: " + shopIdStr);
                        }
                        // 缓存未命中则回源 product-service 查询店铺归属
                        Long userShopId = cacheService.hGetOrLoad(
                                "user:shop:mapping", userId.toString(), null, Long.class,
                                () -> {
                                    try {
                                        R<Long> r = productClient.getShopIdByOwner(userId);
                                        return (r != null && r.getData() != null) ? r.getData() : null;
                                    } catch (Exception e) {
                                        log.error("回源查询店铺失败: userId={}", userId, e);
                                        return null;
                                    }
                                });
                        // 检查所有权：用户必须有店铺，且订阅的店铺必须是自己的店铺，管理员例外
                        Object roleObj = accessor.getSessionAttributes().get("role");
                        int role = roleObj instanceof Integer ? (Integer) roleObj : 0;
                        if (role != 2) {  // 非管理员
                            if (userShopId == null || userShopId != targetShopId) {
                                log.warn("订阅被拒绝: userId={}, 请求 shopId={}, 所属 shopId={}", userId, targetShopId, userShopId);
                                throw new IllegalArgumentException("无权订阅该店铺的消息");
                            }
                        }
                        log.debug("WebSocket 订阅授权: userId={}, dest={}", userId, dest);
                    }
                }
                return message;
            }
        });
    }
}
