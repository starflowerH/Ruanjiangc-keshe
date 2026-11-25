package com.demo.miniprogram.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类 - Token生成、解析、校验
 *
 * JWT Payload 结构：
 *   sub  → 用户标识（openid 或 admin_xxx）
 *   type → 用户类型（volunteer / high_school_student / admin）
 *   rid  → 关联ID（志愿者ID/高中生ID/管理员ID）
 *   role → 管理角色（SUPER_ADMIN / NORMAL_ADMIN / TEACHER），仅admin用户有
 *   uid  → 大学ID（TEACHER角色的数据隔离依据），仅admin用户有
 *   iat  → 签发时间
 *   exp  → 过期时间
 *
 * @author demo
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret:demo-secret-key-for-development-min-256-bits-long!!}")
    private String secretKey;

    @Getter
    @Value("${jwt.expire-seconds:604800}")
    private Long expireSeconds;

    /**
     * 生成JWT Token（完整参数 - 管理员登录用）
     *
     * @param subject      用户标识
     * @param userType     用户类型
     * @param relatedId    关联业务ID
     * @param role         管理角色（仅admin，普通用户传null）
     * @param universityId 大学ID（仅TEACHER角色，其余传null）
     */
    public String generateToken(String subject, String userType, Integer relatedId,
                                 String role, Integer universityId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expireSeconds * 1000);

        Map<String, Object> claims = new HashMap<>();
        claims.put("type", userType);
        if (relatedId != null)  claims.put("rid", relatedId);
        if (role != null)       claims.put("role", role);
        if (universityId != null) claims.put("uid", universityId);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }

    /** 生成JWT Token（含关联ID，无角色信息） */
    public String generateToken(String subject, String userType, Integer relatedId) {
        return generateToken(subject, userType, relatedId, null, null);
    }

    /** 生成JWT Token（无关联ID） */
    public String generateToken(String subject, String userType) {
        return generateToken(subject, userType, null, null, null);
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException e) {
            log.warn("JWT校验失败: {}", e.getMessage());
            return false;
        }
    }

    public String getSubject(String token) {
        return parseClaims(token).getSubject();
    }

    public String getUserType(String token) {
        return parseClaims(token).get("type", String.class);
    }

    public Integer getRelatedId(String token) {
        Object rid = parseClaims(token).get("rid");
        if (rid == null) return null;
        return ((Number) rid).intValue();
    }

    /**
     * 获取管理角色（仅admin用户有值）
     * @return SUPER_ADMIN / NORMAL_ADMIN / TEACHER，普通用户返回null
     */
    public String getRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    /**
     * 获取关联的大学ID（仅TEACHER角色有值）
     * @return 大学ID，非TEACHER角色返回null
     */
    public Integer getUniversityId(String token) {
        Object uid = parseClaims(token).get("uid");
        if (uid == null) return null;
        return ((Number) uid).intValue();
    }

    /* ========== 私有方法 ========== */

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, Math.min(keyBytes.length, 32));
            keyBytes = padded;
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
