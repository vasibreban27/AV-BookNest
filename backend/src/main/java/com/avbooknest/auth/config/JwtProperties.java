package com.avbooknest.auth.config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "app.jwt")
@Validated
public class JwtProperties {

    @NotBlank
    private String secret;

    private Duration accessTokenExpiration = Duration.ofMinutes(15);

    private Duration refreshTokenExpiration = Duration.ofDays(7);

    @PostConstruct
    void validateSecret() {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 characters");
        }
    }

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public Duration getAccessTokenExpiration() { return accessTokenExpiration; }
    public void setAccessTokenExpiration(Duration accessTokenExpiration) { this.accessTokenExpiration = accessTokenExpiration; }
    public Duration getRefreshTokenExpiration() { return refreshTokenExpiration; }
    public void setRefreshTokenExpiration(Duration refreshTokenExpiration) { this.refreshTokenExpiration = refreshTokenExpiration; }
}
