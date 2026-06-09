CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    info VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT REFERENCES users(id),
    "role" VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, "role")
);

CREATE TABLE IF NOT EXISTS clients (
    id SERIAL PRIMARY KEY,
    client_id VARCHAR(255) UNIQUE NOT NULL,
    client_secret_hash VARCHAR(255) NOT NULL,
    audience VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS client_grants (
    client_id BIGINT REFERENCES clients(id),
    "grant" VARCHAR(50) NOT NULL,
    PRIMARY KEY (client_id, "grant")
);

CREATE TABLE IF NOT EXISTS client_scopes (
    client_id BIGINT REFERENCES clients(id),
    "scope" VARCHAR(100) NOT NULL,
    PRIMARY KEY (client_id, "scope")
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id SERIAL PRIMARY KEY,
    refresh_id VARCHAR(255) UNIQUE NOT NULL,
    user_id VARCHAR(100) NOT NULL,
    client_id VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    rotated BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS revocations (
    id SERIAL PRIMARY KEY,
    token_id VARCHAR(255) NOT NULL,
    token_type VARCHAR(20) NOT NULL,
    revoked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP
);


CREATE UNIQUE INDEX IF NOT EXISTS idx_revocations_token ON revocations(token_id, token_type);

CREATE TABLE IF NOT EXISTS metrics (
    id SERIAL PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    grant_type VARCHAR(50),
    client_id VARCHAR(255),
    user_id VARCHAR(100),
    success BOOLEAN NOT NULL,
    error_code VARCHAR(20),
    details VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);