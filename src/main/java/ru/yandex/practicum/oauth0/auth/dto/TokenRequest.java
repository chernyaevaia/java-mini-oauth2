package ru.yandex.practicum.oauth0.auth.dto;

import lombok.Data;
import java.util.List;

@Data
public class TokenRequest {
    private String grantType;
    private String username;
    private String password;
    private String clientId;
    private String clientSecret;
    private List<String> scopes;
    private String refreshToken;
}