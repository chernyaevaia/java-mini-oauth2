package ru.yandex.practicum.oauth0.auth;

import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.classic.HttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import ru.yandex.practicum.oauth0.auth.dto.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = AuthApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TokenIntegrationTest {

    @Value("${local.server.port}")
    private int port;

    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        HttpClient httpClient = HttpClients.custom()
            .disableAutomaticRetries()
            .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        factory.setBufferRequestBody(true);

        this.restTemplate = new RestTemplate(factory);
        this.restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory("http://localhost:" + port));

        this.restTemplate.setErrorHandler(new org.springframework.web.client.ResponseErrorHandler() {
            @Override
            public boolean hasError(org.springframework.http.client.ClientHttpResponse response) throws IOException {
                return false;
            }

            @Override
            public void handleError(org.springframework.http.client.ClientHttpResponse response) throws IOException {
            }
        });
    }

    @Test
    void testPasswordGrantSuccess() {
        TokenRequest request = new TokenRequest();
        request.setGrantType(GrantType.PASSWORD);
        request.setUsername("alice");
        request.setPassword("pass");
        request.setClientId("cli-001");
        request.setClientSecret("secret");
        request.setScopes(List.of("payments:read"));

        ResponseEntity<TokenResponse> response = restTemplate.postForEntity("/token", request, TokenResponse.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getAccessToken());
        assertNotNull(response.getBody().getRefreshToken());
    }

    @Test
    void testPasswordGrantInvalidPassword() {
        TokenRequest request = new TokenRequest();
        request.setGrantType(GrantType.PASSWORD);
        request.setUsername("alice");
        request.setPassword("wrongpass");
        request.setClientId("cli-001");
        request.setClientSecret("secret");

        ResponseEntity<Map> response = restTemplate.postForEntity("/token", request, Map.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testClientCredentialsSuccess() {
        TokenRequest request = new TokenRequest();
        request.setGrantType(GrantType.CLIENT_CREDENTIALS);
        request.setClientId("cli-002");
        request.setClientSecret("svc-secret");
        request.setScopes(List.of("payments:read"));

        ResponseEntity<TokenResponse> response = restTemplate.postForEntity("/token", request, TokenResponse.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getAccessToken());
        assertNull(response.getBody().getRefreshToken());
    }

    @Test
    void testRefreshTokenRotation() {
        TokenRequest req = new TokenRequest();
        req.setGrantType(GrantType.PASSWORD);
        req.setUsername("alice");
        req.setPassword("pass");
        req.setClientId("cli-001");
        req.setClientSecret("secret");

        ResponseEntity<TokenResponse> tokenResp = restTemplate.postForEntity("/token", req, TokenResponse.class);
        assertEquals(HttpStatus.OK, tokenResp.getStatusCode());
        TokenResponse tokens = tokenResp.getBody();
        assertNotNull(tokens);

        TokenRequest refreshReq = new TokenRequest();
        refreshReq.setGrantType(GrantType.REFRESH_TOKEN);
        refreshReq.setRefreshToken(tokens.getRefreshToken());
        refreshReq.setClientId("cli-001");
        refreshReq.setClientSecret("secret");

        ResponseEntity<TokenResponse> resp = restTemplate.postForEntity("/token/refresh", refreshReq, TokenResponse.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertNotNull(resp.getBody().getAccessToken());
        assertNotNull(resp.getBody().getRefreshToken());

        ResponseEntity<Map> retry = restTemplate.postForEntity("/token/refresh", refreshReq, Map.class);
        assertEquals(HttpStatus.CONFLICT, retry.getStatusCode());
    }

    @Test
    void testRefreshTokenRevocation() {
        TokenRequest req = new TokenRequest();
        req.setGrantType(GrantType.PASSWORD);
        req.setUsername("alice");
        req.setPassword("pass");
        req.setClientId("cli-001");
        req.setClientSecret("secret");

        ResponseEntity<TokenResponse> tokenResp = restTemplate.postForEntity("/token", req, TokenResponse.class);
        assertEquals(HttpStatus.OK, tokenResp.getStatusCode());
        TokenResponse tokens = tokenResp.getBody();
        assertNotNull(tokens);

        RevokeRequest revoke = new RevokeRequest();
        revoke.setToken(tokens.getRefreshToken());
        revoke.setTokenTypeHint("refresh_token");
        ResponseEntity<Map> revokeResp = restTemplate.postForEntity("/revoke", revoke, Map.class);
        assertEquals(HttpStatus.OK, revokeResp.getStatusCode());

        TokenRequest refreshReq = new TokenRequest();
        refreshReq.setGrantType(GrantType.REFRESH_TOKEN);
        refreshReq.setRefreshToken(tokens.getRefreshToken());
        refreshReq.setClientId("cli-001");
        refreshReq.setClientSecret("secret");

        ResponseEntity<Map> resp = restTemplate.postForEntity("/token/refresh", refreshReq, Map.class);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void testAccessTokenRevocation() {
        TokenRequest req = new TokenRequest();
        req.setGrantType(GrantType.PASSWORD);
        req.setUsername("alice");
        req.setPassword("pass");
        req.setClientId("cli-001");
        req.setClientSecret("secret");

        ResponseEntity<TokenResponse> tokenResp = restTemplate.postForEntity("/token", req, TokenResponse.class);
        assertEquals(HttpStatus.OK, tokenResp.getStatusCode());
        TokenResponse tokens = tokenResp.getBody();
        assertNotNull(tokens);

        RevokeRequest revoke = new RevokeRequest();
        revoke.setToken(tokens.getAccessToken());
        revoke.setTokenTypeHint("access_token");
        ResponseEntity<Map> revokeResp = restTemplate.postForEntity("/revoke", revoke, Map.class);
        assertEquals(HttpStatus.OK, revokeResp.getStatusCode());

        IntrospectRequest introspect = new IntrospectRequest();
        introspect.setToken(tokens.getAccessToken());
        ResponseEntity<IntrospectResponse> introRespEntity = restTemplate.postForEntity("/introspect", introspect, IntrospectResponse.class);

        assertEquals(HttpStatus.OK, introRespEntity.getStatusCode());
        IntrospectResponse introResp = introRespEntity.getBody();
        assertNotNull(introResp);
        assertFalse(introResp.isActive());
    }

    @Test
    void testIntrospectionActive() {
        TokenRequest req = new TokenRequest();
        req.setGrantType(GrantType.PASSWORD);
        req.setUsername("alice");
        req.setPassword("pass");
        req.setClientId("cli-001");
        req.setClientSecret("secret");
        req.setScopes(List.of("payments:read"));

        ResponseEntity<TokenResponse> tokenResp = restTemplate.postForEntity("/token", req, TokenResponse.class);
        assertEquals(HttpStatus.OK, tokenResp.getStatusCode());
        TokenResponse tokens = tokenResp.getBody();
        assertNotNull(tokens);

        IntrospectRequest introspect = new IntrospectRequest();
        introspect.setToken(tokens.getAccessToken());
        ResponseEntity<IntrospectResponse> responseEntity = restTemplate.postForEntity("/introspect", introspect, IntrospectResponse.class);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        IntrospectResponse response = responseEntity.getBody();
        assertNotNull(response);
        assertTrue(response.isActive());
        assertTrue(response.getScopes().contains("payments:read"));
        assertNotNull(response.getSub());
        assertTrue(response.getSub().startsWith("u-"));
    }

    @Test
    void testIntrospectionExpiredToken() {
        IntrospectRequest introspect = new IntrospectRequest();
        introspect.setToken("eyJ0eXAiOiJBVCIsImFsZyI6IkhTMjU2In0.eyJleHAiOjF9.signature");
        ResponseEntity<IntrospectResponse> responseEntity = restTemplate.postForEntity("/introspect", introspect, IntrospectResponse.class);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        IntrospectResponse response = responseEntity.getBody();
        assertNotNull(response);
        assertFalse(response.isActive());
    }

    @Test
    void testInsufficientScope() {
        TokenRequest req = new TokenRequest();
        req.setGrantType(GrantType.PASSWORD);
        req.setUsername("bob");
        req.setPassword("secret");
        req.setClientId("cli-001");
        req.setClientSecret("secret");
        req.setScopes(List.of("payments:write"));

        ResponseEntity<Map> response = restTemplate.postForEntity("/token", req, Map.class);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void testInvalidSignature() {
        IntrospectRequest introspect = new IntrospectRequest();
        introspect.setToken("abc.def.ghi");
        ResponseEntity<IntrospectResponse> responseEntity = restTemplate.postForEntity("/introspect", introspect, IntrospectResponse.class);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        IntrospectResponse response = responseEntity.getBody();
        assertNotNull(response);
        assertFalse(response.isActive());
    }

    @Test
    void testTokenHasCorrectScopesForRs() {
        TokenRequest req = new TokenRequest();
        req.setGrantType(GrantType.PASSWORD);
        req.setUsername("alice");
        req.setPassword("pass");
        req.setClientId("cli-001");
        req.setClientSecret("secret");
        req.setScopes(List.of("payments:read"));

        ResponseEntity<TokenResponse> tokenResp = restTemplate.postForEntity("/token", req, TokenResponse.class);
        assertEquals(HttpStatus.OK, tokenResp.getStatusCode());
        TokenResponse tokens = tokenResp.getBody();
        assertNotNull(tokens);

        IntrospectRequest introspect = new IntrospectRequest();
        introspect.setToken(tokens.getAccessToken());
        ResponseEntity<IntrospectResponse> introEntity = restTemplate.postForEntity("/introspect", introspect, IntrospectResponse.class);

        assertEquals(HttpStatus.OK, introEntity.getStatusCode());
        IntrospectResponse intro = introEntity.getBody();
        assertNotNull(intro);
        assertTrue(intro.isActive());
        assertTrue(intro.getScopes().contains("payments:read"));
        assertEquals("payments-api", intro.getAud());
    }

    @Test
    void testTokenWithoutWriteScope() {
        TokenRequest req = new TokenRequest();
        req.setGrantType(GrantType.PASSWORD);
        req.setUsername("alice");
        req.setPassword("pass");
        req.setClientId("cli-001");
        req.setClientSecret("secret");
        req.setScopes(List.of("payments:read"));

        ResponseEntity<TokenResponse> tokenResp = restTemplate.postForEntity("/token", req, TokenResponse.class);
        assertEquals(HttpStatus.OK, tokenResp.getStatusCode());
        TokenResponse tokens = tokenResp.getBody();
        assertNotNull(tokens);

        IntrospectRequest introspect = new IntrospectRequest();
        introspect.setToken(tokens.getAccessToken());
        ResponseEntity<IntrospectResponse> introEntity = restTemplate.postForEntity("/introspect", introspect, IntrospectResponse.class);

        assertEquals(HttpStatus.OK, introEntity.getStatusCode());
        IntrospectResponse intro = introEntity.getBody();
        assertNotNull(intro);
        assertFalse(intro.getScopes().contains("payments:write"));
    }
}