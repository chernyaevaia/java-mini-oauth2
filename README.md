# Mini-OAuth2

Educational OAuth2 authorization server with Resource Server.

## Features
- Token issuance (password, client_credentials)
- Token refresh with rotation
- Token revocation and introspection
- Local JWT validation on Resource Server (HS256 + scopes)
- State persistence in database

# Run

## Auth Server (port 8080)
`mvn exec:java -Dexec.mainClass="ru.yandex.practicum.oauth0.auth.AuthApp"`

## Resource Server (port 9090)
`mvn exec:java -Dexec.mainClass="ru.yandex.practicum.oauth0.rs.ResourceApp" -Dexec.args="--server.port=9090"`

1. Get token (password)
   
`curl -s -X POST http://localhost:8080/token \
  -H 'Content-Type: application/json' \
  -d '{
    "grantType": "password",
    "username": "alice",
    "password": "pass",
    "clientId": "cli-001",
    "clientSecret": "secret",
    "scopes": ["payments:read"]
  }'`

2. Access resource

`curl -s -X GET http://localhost:9090/api/payments \
  -H "Authorization: Bearer <insert_access_token>"`
  
3. Refresh token

`curl -s -X POST http://localhost:8080/token/refresh \
  -H 'Content-Type: application/json' \
  -d '{
    "grantType": "refresh_token",
    "refreshToken": "<insert_refresh_token>",
    "clientId": "cli-001",
    "clientSecret": "secret"
  }'`
  
4. Revoke access token

`curl -s -X POST http://localhost:8080/revoke \
  -H 'Content-Type: application/json' \
  -d '{
    "token": "<insert_access_token>",
    "tokenTypeHint": "access_token"
  }'`
  
5. Revoke refresh token

`curl -s -X POST http://localhost:8080/revoke \
  -H 'Content-Type: application/json' \
  -d '{
    "token": "<insert_refresh_token>",
    "tokenTypeHint": "refresh_token"
  }'`
  
# Endpoints
## Auth Server (port 8080)

- POST /token — Issue tokens
- POST /token/refresh — Refresh tokens
- POST /revoke — Revoke token
- POST /introspect — Check token status

## Resource Server (port 9090)

- GET /api/payments — Required scope: payments:read
- POST /api/payments — Required scope: payments:write

