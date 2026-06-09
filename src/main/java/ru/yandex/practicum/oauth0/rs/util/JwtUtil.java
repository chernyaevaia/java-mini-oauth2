package ru.yandex.practicum.oauth0.rs.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

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

    private Map<String, Object> parseJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }

    public static class TokenException extends Exception {
        public TokenException(String message) {
            super(message);
        }
    }
}