package ru.yandex.practicum.oauth0.rs;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "rs")
public class RsProperties {
    private String authServerUrl = "http://localhost:8080";
    private String audience = "payments-api";
    private String authSecret;
    private long clockSkewSec = 60;

    public String getAuthSecret() {
        return authSecret;
    }

    public String getAuthServerUrl() {
        return authServerUrl;
    }

    public void setAuthServerUrl(String authServerUrl) {
        this.authServerUrl = authServerUrl;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public void setAuthSecret(String authSecret) {
        this.authSecret = authSecret;
    }

    public long getClockSkewSec() {
        return clockSkewSec;
    }

    public void setClockSkewSec(long clockSkewSec) {
        this.clockSkewSec = clockSkewSec;
    }
}