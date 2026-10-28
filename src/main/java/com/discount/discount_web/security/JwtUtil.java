package com.discount.discount_web.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    // JWT signing key comes only from the JWT_SECRET environment variable; rotating it invalidates all old tokens.
    @Value("${JWT_SECRET:}")
    private String secret;

    @PostConstruct
    public void initSecret() {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET is not set or shorter than 32 chars, refusing to start");
        }
    }
    
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // 生成 7 日有效期嘅 JWT Token
    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000)) 
                .signWith(getSigningKey())
                .compact();
    }

    // 驗證 Token，無效或過期會自動拋出 Exception
    public String validateTokenAndGetUsername(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }
}