package ru.yandex.practicum.oauth0.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth")
public class AuthProperties {
    private String secret;
    private String issuer = "mini-auth";
    private long accessTtlSec = 900;
    private long refreshTtlDays = 14;
    private long clockSkewSec = 60;
    private String audience = "payments-api";

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }

    public long getAccessTtlSec() { return accessTtlSec; }
    public void setAccessTtlSec(long accessTtlSec) { this.accessTtlSec = accessTtlSec; }

    public long getRefreshTtlDays() { return refreshTtlDays; }
    public void setRefreshTtlDays(long refreshTtlDays) { this.refreshTtlDays = refreshTtlDays; }

    public long getClockSkewSec() { return clockSkewSec; }
    public void setClockSkewSec(long clockSkewSec) { this.clockSkewSec = clockSkewSec; }

    public String getAudience() { return audience; }
    public void setAudience(String audience) { this.audience = audience; }
}