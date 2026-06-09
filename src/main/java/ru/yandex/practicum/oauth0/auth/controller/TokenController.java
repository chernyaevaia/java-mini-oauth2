package ru.yandex.practicum.oauth0.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.oauth0.auth.dto.*;
import ru.yandex.practicum.oauth0.auth.service.TokenService;

import java.util.Map;

@RestController
public class TokenController {

    private final TokenService tokenService;

    public TokenController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @PostMapping("/token")
    public ResponseEntity<?> token(@RequestBody TokenRequest request) {
        String grantType = request.getGrantType();

        if ("password".equals(grantType)) {
            return ResponseEntity.ok(tokenService.issueTokenPassword(request));
        } else if ("client_credentials".equals(grantType)) {
            return ResponseEntity.ok(tokenService.issueTokenClientCredentials(request));
        } else {
            return ResponseEntity.status(400)
                .body(Map.of("error", "unsupported_grant_type"));
        }
    }

    @PostMapping("/token/refresh")
    public ResponseEntity<?> refresh(@RequestBody TokenRequest request) {
        if (!"refresh_token".equals(request.getGrantType())) {
            return ResponseEntity.status(400)
                .body(Map.of("error", "invalid_grant_type"));
        }
        return ResponseEntity.ok(tokenService.refreshToken(request));
    }

    @PostMapping("/revoke")
    public ResponseEntity<?> revoke(@RequestBody RevokeRequest request) {
        tokenService.revoke(request);
        return ResponseEntity.ok(Map.of("revoked", true));
    }

    @PostMapping("/introspect")
    public ResponseEntity<IntrospectResponse> introspect(@RequestBody IntrospectRequest request) {
        return ResponseEntity.ok(tokenService.introspect(request));
    }

    @ExceptionHandler(TokenService.AuthException.class)
    public ResponseEntity<Map<String, String>> handleAuthException(TokenService.AuthException e) {
        return ResponseEntity.status(e.getStatus())
            .body(Map.of("error", e.getMessage()));
    }
}