package ru.yandex.practicum.oauth0.auth.service;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.oauth0.auth.config.AuthProperties;
import ru.yandex.practicum.oauth0.auth.dto.*;
import ru.yandex.practicum.oauth0.auth.model.*;
import ru.yandex.practicum.oauth0.auth.repository.*;
import ru.yandex.practicum.oauth0.auth.util.JwtUtil;
import ru.yandex.practicum.oauth0.auth.model.Metrics;
import ru.yandex.practicum.oauth0.auth.repository.MetricsRepository;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    private final MetricsRepository metricsRepository;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RevocationRepository revocationRepository;
    private final JwtUtil jwtUtil;
    private final AuthProperties props;

    public TokenService(MetricsRepository metricsRepository,
                      UserRepository userRepository,
                      ClientRepository clientRepository,
                      RefreshTokenRepository refreshTokenRepository,
                      RevocationRepository revocationRepository,
                      JwtUtil jwtUtil,
                      AuthProperties props) {
        this.metricsRepository = metricsRepository;
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.revocationRepository = revocationRepository;
        this.jwtUtil = jwtUtil;
        this.props = props;
    }

    public TokenResponse issueTokenPassword(TokenRequest request) {
        try {
            Client client = authenticateClient(request.getClientId(), request.getClientSecret());
            if (!client.getAllowedGrants().contains("password")) {
                logMetrics("token_issued", "password", request.getClientId(), null, false, "400", "grant not allowed");
                throw new AuthException("Grant type 'password' not allowed", 400);
            }

            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> {
                        logMetrics("token_issued", "password", request.getClientId(), null, false, "401", "invalid user");
                        return new AuthException("Invalid username or password", 401);
                    });

            if (!BCrypt.checkpw(request.getPassword(), user.getPasswordHash())) {
                logMetrics("token_issued", "password", request.getClientId(), "u-" + user.getId(), false, "401", "invalid password");
                throw new AuthException("Invalid username or password", 401);
            }

            List<String> scopes = resolveScopes(request.getScopes(), client.getAllowedScopes(), user.getRoles());

            if (request.getScopes() != null && !request.getScopes().isEmpty() && scopes.isEmpty()) {
                logMetrics("token_issued", "password", request.getClientId(), "u-" + user.getId(), false, "403", "scopes not allowed");
                throw new AuthException("Requested scopes not allowed for this user", 403);
            }

            String jti = UUID.randomUUID().toString();
            String refreshId = UUID.randomUUID().toString();

            String accessToken = jwtUtil.createAccessToken(
                    props.getIssuer(), props.getAudience(),
                    "u-" + user.getId(), client.getClientId(),
                    scopes, user.getRoles(),
                    props.getAccessTtlSec(), jti, null);

            String refreshToken = jwtUtil.createRefreshToken(
                    props.getIssuer(), "u-" + user.getId(),
                    client.getClientId(), props.getRefreshTtlDays(), refreshId);

            RefreshToken rt = new RefreshToken();
            rt.setRefreshId(refreshId);
            rt.setUserId("u-" + user.getId());
            rt.setClientId(client.getClientId());
            rt.setExpiresAt(Instant.now().plusSeconds(props.getRefreshTtlDays() * 24L * 60 * 60));
            refreshTokenRepository.save(rt);

            logMetrics("token_issued", "password", client.getClientId(), "u-" + user.getId(), true, null, null);
            log.info("Token issued: grant=password, clientId={}, userId={}", client.getClientId(), "u-" + user.getId());

            return new TokenResponse(accessToken, refreshToken, props.getAccessTtlSec());

        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during password token issuance", e);
            logMetrics("token_issued", "password", request.getClientId(), null, false, "500", "internal error");
            throw new AuthException("Internal error", 500);
        }
    }

    public TokenResponse issueTokenClientCredentials(TokenRequest request) {
        try {
            Client client = authenticateClient(request.getClientId(), request.getClientSecret());
            if (!client.getAllowedGrants().contains("client_credentials")) {
                logMetrics("token_issued", "client_credentials", request.getClientId(), null, false, "400", "grant not allowed");
                throw new AuthException("Grant type 'client_credentials' not allowed", 400);
            }

            List<String> scopes = resolveScopes(request.getScopes(), client.getAllowedScopes(), Collections.emptyList());
            String jti = UUID.randomUUID().toString();

            String accessToken = jwtUtil.createAccessToken(
                    props.getIssuer(), props.getAudience(),
                    client.getClientId(), client.getClientId(),
                    scopes, Collections.emptyList(),
                    props.getAccessTtlSec(), jti, null);

            logMetrics("token_issued", "client_credentials", client.getClientId(), null, true, null, null);
            log.info("Token issued: grant=client_credentials, clientId={}", client.getClientId());

            return new TokenResponse(accessToken, null, props.getAccessTtlSec());

        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during client_credentials token issuance", e);
            logMetrics("token_issued", "client_credentials", request.getClientId(), null, false, "500", "internal error");
            throw new AuthException("Internal error", 500);
        }
    }

    public TokenResponse refreshToken(TokenRequest request) {
        try {
            Client client = authenticateClient(request.getClientId(), request.getClientSecret());

            Map<String, Object> payload;
            try {
                payload = jwtUtil.verifyAndDecode(request.getRefreshToken());
            } catch (Exception e) {
                logMetrics("token_refresh", "refresh_token", request.getClientId(), null, false, "401", "invalid refresh token");
                throw new AuthException("Invalid refresh token", 401);
            }

            String refreshId = Objects.toString(payload.get("refresh_id"), null);
            if (refreshId == null) {
                logMetrics("token_refresh", "refresh_token", request.getClientId(), null, false, "401", "missing refresh_id");
                throw new AuthException("Invalid refresh token", 401);
            }

            if (revocationRepository.findByTokenIdAndTokenType(refreshId, "refresh").isPresent()) {
                logMetrics("token_refresh", "refresh_token", request.getClientId(), null, false, "401", "refresh revoked");
                throw new AuthException("Refresh token revoked", 401);
            }

            RefreshToken rt = refreshTokenRepository.findByRefreshId(refreshId)
                    .orElseThrow(() -> {
                        logMetrics("token_refresh", "refresh_token", request.getClientId(), null, false, "401", "refresh not found");
                        return new AuthException("Refresh token not found", 401);
                    });

            if (rt.isRotated()) {
                logMetrics("token_refresh", "refresh_token", request.getClientId(), rt.getUserId(), false, "409", "refresh reused");
                throw new AuthException("Refresh token already used", 409);
            }

            Instant now = Instant.now();
            Instant skewedNow = now.minusSeconds(props.getClockSkewSec());
            if (rt.getExpiresAt().isBefore(skewedNow)) {
                logMetrics("token_refresh", "refresh_token", request.getClientId(), rt.getUserId(), false, "401", "refresh expired");
                throw new AuthException("Refresh token expired", 401);
            }

            rt.setRotated(true);
            refreshTokenRepository.save(rt);

            String userId = (String) payload.get("sub");
            String newJti = UUID.randomUUID().toString();
            String newRefreshId = UUID.randomUUID().toString();

            List<String> roles = Collections.emptyList();
            if (userId != null && userId.startsWith("u-")) {
                try {
                    long uid = Long.parseLong(userId.substring(2));
                    roles = userRepository.findById(uid).map(User::getRoles).orElse(Collections.emptyList());
                } catch (Exception ignored) {
                }
            }

            List<String> scopes = resolveScopes(null, client.getAllowedScopes(), roles);

            String accessToken = jwtUtil.createAccessToken(
                    props.getIssuer(), props.getAudience(),
                    userId, client.getClientId(),
                    scopes, roles,
                    props.getAccessTtlSec(), newJti, null);

            String refreshToken = jwtUtil.createRefreshToken(
                    props.getIssuer(), userId,
                    client.getClientId(), props.getRefreshTtlDays(), newRefreshId);

            RefreshToken newRt = new RefreshToken();
            newRt.setRefreshId(newRefreshId);
            newRt.setUserId(userId);
            newRt.setClientId(client.getClientId());
            newRt.setExpiresAt(Instant.now().plusSeconds(props.getRefreshTtlDays() * 24L * 60 * 60));
            refreshTokenRepository.save(newRt);

            logMetrics("token_refresh", "refresh_token", client.getClientId(), userId, true, null, null);
            log.info("Token refreshed: clientId={}, userId={}", client.getClientId(), userId);

            return new TokenResponse(accessToken, refreshToken, props.getAccessTtlSec());

        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during token refresh", e);
            logMetrics("token_refresh", "refresh_token", request.getClientId(), null, false, "500", "internal error");
            throw new AuthException("Internal error", 500);
        }
    }

    public IntrospectResponse introspect(IntrospectRequest request) {
        IntrospectResponse response = new IntrospectResponse();
        Map<String, Object> payload;
        try {
            payload = jwtUtil.verifyAndDecode(request.getToken());
            if (payload == null)
                return inactiveResponse(response);
        } catch (Exception e) {
            return inactiveResponse(response);
        }

        long now = Instant.now().getEpochSecond();
        long exp = ((Number) payload.get("exp")).longValue();
        long iat = ((Number) payload.get("iat")).longValue();
        long skew = props.getClockSkewSec();

        if (now > exp + skew || now < iat - skew) {
            return inactiveResponse(response);
        }

        String jti = java.util.Objects.toString(payload.get("jti"), null);
        String tokenType = java.util.Objects.toString(payload.get("typ"), null);
        String checkId = "RT".equals(tokenType) ? java.util.Objects.toString(payload.get("refresh_id"), null) : jti;
        String checkType = "RT".equals(tokenType) ? "refresh" : "access";

        if (checkId != null && revocationRepository.findByTokenIdAndTokenType(checkId, checkType).isPresent()) {
            return inactiveResponse(response);
        }

        response.setActive(true);
        response.setSub(java.util.Objects.toString(payload.get("sub"), null));
        response.setAud(java.util.Objects.toString(payload.get("aud"), null));
        response.setClientId(java.util.Objects.toString(payload.get("client_id"), null));
        response.setScopes(asStringList(payload.get("scopes")));
        response.setRoles(asStringList(payload.get("roles")));

        if (payload.containsKey("exp"))
            response.setExp(((Number) payload.get("exp")).longValue());
        if (payload.containsKey("iat"))
            response.setIat(((Number) payload.get("iat")).longValue());

        return response;
    }

    private IntrospectResponse inactiveResponse(IntrospectResponse r) {
        r.setActive(false);
        return r;
    }

    private List<String> asStringList(Object obj) {
        if (obj == null)
            return List.of();
        if (obj instanceof java.util.Collection) {
            return ((java.util.Collection<?>) obj).stream().map(Object::toString)
                    .collect(java.util.stream.Collectors.toList());
        }
        if (obj instanceof String) {
            String s = (String) obj;
            return s.isEmpty() ? List.of() : java.util.Arrays.asList(s.split(" "));
        }
        return List.of();
    }

    public void revoke(RevokeRequest request) {
        try {
            String tokenTypeHint = request.getTokenTypeHint();
            String detectedType = jwtUtil.extractType(request.getToken());

            boolean isRefresh = "refresh_token".equals(tokenTypeHint)
                    || ("RT".equals(detectedType) && tokenTypeHint == null);
            String tokenId = isRefresh ? jwtUtil.extractRefreshId(request.getToken())
                    : jwtUtil.extractJti(request.getToken());

            if (tokenId == null) {
                throw new AuthException("Invalid token format", 400);
            }

            Revocation rev = new Revocation();
            rev.setTokenId(tokenId);
            rev.setTokenType(isRefresh ? "refresh" : "access");

            Map<String, Object> payload = jwtUtil.decodeWithoutVerify(request.getToken());
            if (payload != null && payload.containsKey("exp")) {
                rev.setExpiresAt(Instant.ofEpochSecond(((Number) payload.get("exp")).longValue()));
            }
            revocationRepository.save(rev);

            if (isRefresh) {
                refreshTokenRepository.findByRefreshId(tokenId).ifPresent(rt -> {
                    rt.setRotated(true);
                    refreshTokenRepository.save(rt);
                });
            }

            logMetrics("token_revoked", isRefresh ? "refresh_token" : "access_token", null, null, true, null, null);
            log.info("Token revoked: type={}, tokenId={}", isRefresh ? "refresh" : "access", tokenId);

        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during token revocation", e);
            throw new AuthException("Internal error", 500);
        }
    }

    private Client authenticateClient(String clientId, String clientSecret) {
        if (clientId == null || clientSecret == null) {
            throw new AuthException("Client credentials required", 401);
        }

        Client client = clientRepository.findByClientId(clientId)
                .orElseThrow(() -> new AuthException("Invalid client credentials", 401));

        if (!BCrypt.checkpw(clientSecret, client.getClientSecretHash())) {
            throw new AuthException("Invalid client credentials", 401);
        }

        return client;
    }

    private List<String> resolveScopes(List<String> requested, List<String> allowed, List<String> roles) {
        Map<String, List<String>> roleScopes = Map.of(
                "viewer", List.of("payments:read"),
                "admin", List.of("payments:read", "payments:write", "users:read", "users:write"),
                "editor", List.of("payments:read", "payments:write"));

        Set<String> availableFromRoles = new HashSet<>();
        for (String role : roles) {
            List<String> s = roleScopes.get(role);
            if (s != null)
                availableFromRoles.addAll(s);
        }

        Set<String> available = new HashSet<>(allowed);
        available.retainAll(availableFromRoles);

        if (requested == null || requested.isEmpty()) {
            return new ArrayList<>(available);
        }

        return requested.stream()
                .filter(available::contains)
                .collect(Collectors.toList());
    }

    private void logMetrics(String eventType, String grantType, String clientId, String userId, boolean success, String errorCode, String details) {
        try {
            Metrics metrics = new Metrics();
            metrics.setEventType(eventType);
            metrics.setGrantType(grantType);
            metrics.setClientId(clientId);
            metrics.setUserId(userId);
            metrics.setSuccess(success);
            metrics.setErrorCode(errorCode);
            metrics.setDetails(details);
            metricsRepository.save(metrics);
        } catch (Exception e) {
            log.error("Failed to save metrics", e);
        }
    }

    public static class AuthException extends RuntimeException {
        private final int status;

        public AuthException(String message, int status) {
            super(message);
            this.status = status;
        }

        public int getStatus() {
            return status;
        }
    }
}