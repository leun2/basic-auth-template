package com.leun.auth.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    private final Key key;

    private final long accessTokenExpirationTime;
    private final long refreshTokenExpirationTime;

    public JwtUtil(
        @Value("${jwt.secret}") String secretKey,
        @Value("${jwt.access-token-expiration}") long accessTokenExpirationTime,
        @Value("${jwt.refresh-token-expiration}") long refreshTokenExpirationTime) {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
        this.accessTokenExpirationTime = accessTokenExpirationTime;
        this.refreshTokenExpirationTime = refreshTokenExpirationTime;
    }

    public String generateAccessToken(String email) {

        return Jwts.builder()
            .setSubject(email)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpirationTime))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }

    public String generateRefreshToken(String email) {
        return Jwts.builder()
            .setSubject(email)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpirationTime)) // Refresh Token 만료 시간 사용
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }

    public String extractEmail(String token) {

        return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .getBody()
            .getSubject();
    }

    public boolean validateToken(String token) {

        try {
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

