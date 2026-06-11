DELETE FROM revocations;
DELETE FROM refresh_tokens;
DELETE FROM client_scopes;
DELETE FROM client_grants;
DELETE FROM clients;
DELETE FROM user_roles;
DELETE FROM users;

INSERT INTO users (username, password_hash, info) VALUES 
('alice', '$2a$12$R7dF.orSvorhBMx1Tt02J.yogtGrECU6FBaDzDrvK.sgbhceC.zNG', 'Alice Smith'),
('bob', '$2a$12$Bpo.BMzWwYDm9BLC09xdl.pSQVGmecVpp/B3gJE4qVMIjYlpm3W6S', 'Bob Jones');

INSERT INTO user_roles (user_id, "role") VALUES 
((SELECT id FROM users WHERE username='alice'), 'viewer'),
((SELECT id FROM users WHERE username='bob'), 'viewer');

INSERT INTO clients (client_id, client_secret_hash, audience) VALUES 
('cli-001', '$2a$12$2u2wShFJxXkydkyB4T/e0.LljtRkDUnzTugsPPRSIodMENF7BloO6', 'payments-api'),
('cli-002', '$2a$12$5P6lpZL18CZIzYvMquq5j.xaJWEO1nciWV/GkqEABxwWPXcQzra56', 'payments-api');

INSERT INTO client_grants (client_id, "grant") VALUES 
((SELECT id FROM clients WHERE client_id='cli-001'), 'password'),
((SELECT id FROM clients WHERE client_id='cli-001'), 'refresh_token'),
((SELECT id FROM clients WHERE client_id='cli-002'), 'client_credentials');

INSERT INTO client_scopes (client_id, "scope") VALUES 
((SELECT id FROM clients WHERE client_id='cli-001'), 'payments:read'),
((SELECT id FROM clients WHERE client_id='cli-001'), 'payments:write'),
((SELECT id FROM clients WHERE client_id='cli-002'), 'payments:read');