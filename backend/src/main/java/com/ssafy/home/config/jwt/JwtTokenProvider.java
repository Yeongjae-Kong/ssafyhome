package com.ssafy.home.config.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ssafy.home.model.dto.User;

@Component
public class JwtTokenProvider {
    private final SecretKey secretKey;
    private final long accessValidityMinutes;
    private final long refreshValidityMinutes;
    private final String issuer;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity-in-ms}") long accessValidity,
            @Value("${jwt.refresh-token-validity-in-ms}") long refreshValidity,
            @Value("${jwt.issuer:ssafyhome}") String issuer
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        // 주의: 프로퍼티 명은 in-ms지만 기존 예시와 호환을 위해 분 단위로 해석합니다.
        this.accessValidityMinutes = accessValidity;
        this.refreshValidityMinutes = refreshValidity;
        this.issuer = issuer;
    }

    public String createAccessToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessValidityMinutes * 60_000);
        return Jwts.builder()
                .setSubject(String.valueOf(user.getMno()))
                .setIssuer(issuer)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .claim("email", user.getEmail())
                .claim("name", user.getName())
                .claim("role", user.getRole())
                .claim("type", "ACCESS")
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String createRefreshToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshValidityMinutes * 60_000);
        return Jwts.builder()
                .setSubject(String.valueOf(user.getMno()))
                .setIssuer(issuer)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .claim("type", "REFRESH")
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public void validateToken(String token) throws JwtException, ExpiredJwtException {
        parseClaims(token); // 파싱 실패 시 예외
    }

    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isAccessToken(String token) {
        return "ACCESS".equals(parseClaims(token).get("type", String.class));
    }

    public boolean isRefreshToken(String token) {
        return "REFRESH".equals(parseClaims(token).get("type", String.class));
    }
}

