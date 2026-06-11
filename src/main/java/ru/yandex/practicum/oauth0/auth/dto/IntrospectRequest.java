package ru.yandex.practicum.oauth0.auth.dto;

import lombok.Data;

@Data
public class IntrospectRequest {
    private String token;
    private String tokenTypeHint;
}