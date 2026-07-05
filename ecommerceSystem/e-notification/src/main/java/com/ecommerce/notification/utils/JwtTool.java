package com.ecommerce.notification.utils;

import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTValidator;
import cn.hutool.jwt.signers.JWTSigner;
import cn.hutool.jwt.signers.JWTSignerUtil;
import org.springframework.stereotype.Component;

import java.security.KeyPair;

/**
 * JWT 工具 —— 仅用于验证 token（不生成）。
 * 与 e-gateway 使用相同的 RSA256 密钥对，保证能互相识别对方颁发的 JWT。
 */
@Component
public class JwtTool {

    private final JWTSigner jwtSigner;

    public JwtTool(KeyPair keyPair) {
        // 用 RSA256 密钥对创建签名验证器
        this.jwtSigner = JWTSignerUtil.createSigner("rs256", keyPair);
    }

    /**
     * 验证 token 的有效性（签名、过期时间），并返回其中存储的 userId。
     */
    public Long parseToken(String token) {
        return parseJwt(token).userId;
    }

    /**
     * 验证并解析 JWT，返回 userId 和 role。
     */
    public TokenInfo parseJwt(String token) {
        JWT jwt = JWT.of(token).setSigner(jwtSigner);
        if (!jwt.verify()) {
            throw new RuntimeException("无效的token，签名验证失败");
        }
        try {
            JWTValidator.of(jwt).validateDate();
        } catch (ValidateException e) {
            throw new RuntimeException("token已过期");
        }
        Object userPayload = jwt.getPayload("user");
        if (userPayload == null) {
            throw new RuntimeException("无效的token，缺少用户信息");
        }
        Long userId;
        try {
            userId = Long.valueOf(userPayload.toString());
        } catch (NumberFormatException e) {
            throw new RuntimeException("无效的token，用户信息格式错误");
        }
        Integer role = null;
        Object rolePayload = jwt.getPayload("role");
        if (rolePayload != null) {
            try {
                role = Integer.valueOf(rolePayload.toString());
            } catch (NumberFormatException ignored) {}
        }
        return new TokenInfo(userId, role != null ? role : 0);
    }

    public static class TokenInfo {
        public final Long userId;
        public final int role;
        public TokenInfo(Long userId, int role) {
            this.userId = userId;
            this.role = role;
        }
    }
}
