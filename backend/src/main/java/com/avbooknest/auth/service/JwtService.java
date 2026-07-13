package com.avbooknest.auth.service;

import com.avbooknest.auth.config.JwtProperties;
import com.avbooknest.common.exception.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String ACCESS_TOKEN = "access";
    private static final String REFRESH_TOKEN = "refresh";

    private final JwtProperties jwtProperties;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public String generateAccessToken(UserDetails userDetails) {
        return generateToken(userDetails.getUsername(), ACCESS_TOKEN, jwtProperties.getAccessTokenExpiration().toSeconds());
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return generateToken(userDetails.getUsername(), REFRESH_TOKEN, jwtProperties.getRefreshTokenExpiration().toSeconds());
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public Instant extractExpiration(String token) {
        return parseClaims(token).getExpiration().toInstant();
    }

    public boolean isValidAccessToken(String token, UserDetails userDetails) {
        try {
            Claims claims = parseClaims(token);
            return ACCESS_TOKEN.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))
                    && userDetails.getUsername().equals(claims.getSubject());
        } catch (UnauthorizedException exception) {
            return false;
        }
    }

    public void requireRefreshToken(String token) {
        Claims claims = parseClaims(token);
        if (!REFRESH_TOKEN.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))) {
            throw new UnauthorizedException("Invalid refresh token");
        }
    }

    private String generateToken(String subject, String tokenType, long validitySeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(validitySeconds)))
                .signWith(signingKey())
                .compact();
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException exception) {
            throw new UnauthorizedException("Invalid or expired token");
        }
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
