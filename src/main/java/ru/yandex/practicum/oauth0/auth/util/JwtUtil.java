package ru.yandex.practicum.oauth0.auth.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JwtUtil {

    private final String secret;
    private final long clockSkewSec;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public JwtUtil(String secret, long clockSkewSec) {
        this.secret = secret;
        this.clockSkewSec = clockSkewSec;
    }

    public String createAccessToken(String issuer, String audience, String subject,
            String clientId, List<String> scopes, List<String> roles,
            long accessTtlSec, String jti, String refreshId) {
        long now = Instant.now().getEpochSecond();

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("typ", "AT");
        header.put("alg", "HS256");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("typ", "AT");
        payload.put("iss", issuer);
        payload.put("aud", audience);
        payload.put("sub", subject);
        payload.put("client_id", clientId);
        payload.put("scopes", scopes);
        payload.put("roles", roles);
        payload.put("iat", now);
        payload.put("exp", now + accessTtlSec);
        payload.put("jti", jti);

        return encode(header, payload);
    }

    public String createRefreshToken(String issuer, String subject, String clientId,
            long refreshTtlDays, String refreshId) {
        long now = Instant.now().getEpochSecond();
        long ttlSec = refreshTtlDays * 24 * 60 * 60;

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("typ", "RT");
        header.put("alg", "HS256");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("typ", "RT");
        payload.put("iss", issuer);
        payload.put("sub", subject);
        payload.put("client_id", clientId);
        payload.put("refresh_id", refreshId);
        payload.put("iat", now);
        payload.put("exp", now + ttlSec);
        payload.put("jti", UUID.randomUUID().toString());

        return encode(header, payload);
    }

    public Map<String, Object> verifyAndDecode(String token) throws TokenException {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new TokenException("Invalid token format");
        }

        String toSign = parts[0] + "." + parts[1];
        String expectedSig = signHs256(toSign);
        if (!expectedSig.equals(parts[2])) {
            throw new TokenException("Invalid signature");
        }

        String payloadJson = new String(base64UrlDecode(parts[1]), StandardCharsets.UTF_8);
        Map<String, Object> payload = parseJson(payloadJson);

        long now = Instant.now().getEpochSecond();
        long iat = ((Number) payload.get("iat")).longValue();
        long exp = ((Number) payload.get("exp")).longValue();

        if (now < iat - clockSkewSec) {
            throw new TokenException("Token used before issued");
        }
        if (now > exp + clockSkewSec) {
            throw new TokenException("Token expired");
        }

        return payload;
    }

    public Map<String, Object> decodeWithoutVerify(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3)
                return null;
            String payloadJson = new String(base64UrlDecode(parts[1]), StandardCharsets.UTF_8);
            return parseJson(payloadJson);
        } catch (Exception e) {
            return null;
        }
    }

    public String extractJti(String token) {
        Map<String, Object> payload = decodeWithoutVerify(token);
        if (payload == null)
            return null;
        return (String) payload.get("jti");
    }

    public String extractRefreshId(String token) {
        Map<String, Object> payload = decodeWithoutVerify(token);
        if (payload == null)
            return null;
        return (String) payload.get("refresh_id");
    }

    public String extractType(String token) {
        Map<String, Object> payload = decodeWithoutVerify(token);
        if (payload == null)
            return null;
        return (String) payload.get("typ");
    }

    private String encode(Map<String, Object> header, Map<String, Object> payload) {
        String headerJson = toJson(header);
        String payloadJson = toJson(payload);

        String headerB64 = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8));
        String payloadB64 = base64UrlEncode(payloadJson.getBytes(StandardCharsets.UTF_8));

        String toSign = headerB64 + "." + payloadB64;
        String signature = signHs256(toSign);

        return toSign + "." + signature;
    }

    private String signHs256(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(key);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return base64UrlEncode(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign token", e);
        }
    }

    private String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private byte[] base64UrlDecode(String s) {
        return Base64.getUrlDecoder().decode(s);
    }

    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize JSON", e);
        }
    }

    private Map<String, Object> parseJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public static class TokenException extends Exception {
        public TokenException(String message) {
            super(message);
        }
    }
}