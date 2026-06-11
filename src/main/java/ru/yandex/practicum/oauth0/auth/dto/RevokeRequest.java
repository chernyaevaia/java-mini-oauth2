package ru.yandex.practicum.oauth0.auth.dto;

import lombok.Data;

@Data
public class RevokeRequest {
    private String token;
    private String tokenTypeHint;
}