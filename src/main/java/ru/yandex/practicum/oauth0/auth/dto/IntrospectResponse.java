package ru.yandex.practicum.oauth0.auth.dto;

import java.util.List;

public class IntrospectResponse {
    private boolean active;
    private String sub;
    private String aud; 
    private String clientId;
    private List<String> scopes;
    private List<String> roles;
    private long exp;
    private long iat;
    private String jti;
    private String tokenType;

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getSub() { return sub; }
    public void setSub(String sub) { this.sub = sub; }

    public String getAud() { return aud; } 
    public void setAud(String aud) { this.aud = aud; } 

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public List<String> getScopes() { return scopes; }
    public void setScopes(List<String> scopes) { this.scopes = scopes; }

    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }

    public long getExp() { return exp; }
    public void setExp(long exp) { this.exp = exp; }

    public long getIat() { return iat; }
    public void setIat(long iat) { this.iat = iat; }

    public String getJti() { return jti; }
    public void setJti(String jti) { this.jti = jti; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }
}