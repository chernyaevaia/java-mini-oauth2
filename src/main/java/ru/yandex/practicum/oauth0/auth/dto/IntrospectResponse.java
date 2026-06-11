package ru.yandex.practicum.oauth0.auth.dto;

import lombok.Data;
import java.util.List;

@Data
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
}