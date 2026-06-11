package ru.yandex.practicum.oauth0.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import ru.yandex.practicum.oauth0.auth.service.TokenService;

import java.util.Map;

@ControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(TokenService.AuthException.class)
    public ResponseEntity<Map<String, String>> handleAuthException(TokenService.AuthException e) {
        return ResponseEntity.status(e.getStatus())
                .body(Map.of("error", e.getMessage()));
    }
}