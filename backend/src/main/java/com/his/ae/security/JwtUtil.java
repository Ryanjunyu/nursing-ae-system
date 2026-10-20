package com.his.ae.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/** JWT 工具：生成与解析（jjwt 0.12.x） */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generate(LoginUser u) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", u.getUserId());
        claims.put("role", u.getRole());
        claims.put("deptId", u.getDeptId());
        return Jwts.builder()
                .claims(claims)
                .subject(u.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key())
                .compact();
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Integer getUserId(String token) {
        return parse(token).get("uid", Integer.class);
    }

    public String getRole(String token) {
        return parse(token).get("role", String.class);
    }

    public Integer getDeptId(String token) {
        return parse(token).get("deptId", Integer.class);
    }

    public String getUsername(String token) {
        return parse(token).getSubject();
    }
}
