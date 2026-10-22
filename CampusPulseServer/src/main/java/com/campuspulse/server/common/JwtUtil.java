package com.campuspulse.server.common;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * HS256 JWT：subject 存用户 id，role 区分 USER / ADMIN，默认 7 天有效。
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expireMillis;

    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.expire-days:7}") long expireDays) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireMillis = Duration.ofDays(expireDays).toMillis();
    }

    public String generate(Long subjectId, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(subjectId))
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expireMillis)))
                .signWith(key)
                .compact();
    }

    /**
     * 解析并校验签名与有效期；无效 token 统一抛出 BizException(40101)。
     */
    public TokenPayload parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return new TokenPayload(Long.valueOf(claims.getSubject()), claims.get("role", String.class));
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
    }

    public record TokenPayload(Long userId, String role) {
    }
}
