package com.bfs.hibernateprojectdemo.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;

public class JwtUtil {
    private static final Key KEY = Keys.hmacShaKeyFor(
            "change-this-to-a-very-very-long-256bit-secret-key".getBytes()
    );
    private static final long EXP_MS = 1000L * 60 * 60 * 12; // 12h

    public static String generate(String username, String role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + EXP_MS))
                .signWith(KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    public static Jws<Claims> parse(String token) {
        return Jwts.parserBuilder().setSigningKey(KEY).build().parseClaimsJws(token);
    }
}