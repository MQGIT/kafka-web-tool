-- Create connections table for storing Kafka cluster connection configurations
-- This table stores connection details including security configurations and authentication

CREATE TABLE connections (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    bootstrap_servers VARCHAR(1000) NOT NULL,
    security_protocol VARCHAR(20) NOT NULL DEFAULT 'PLAINTEXT',
    sasl_mechanism VARCHAR(20),
    sasl_jaas_config VARCHAR(1000),
    username VARCHAR(255),
    password VARCHAR(255),
    ssl_truststore_location VARCHAR(500),
    ssl_truststore_password VARCHAR(255),
    ssl_keystore_location VARCHAR(500),
    ssl_keystore_password VARCHAR(255),
    ssl_key_password VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT true,
    connection_timeout_ms INTEGER DEFAULT 30000,
    request_timeout_ms INTEGER DEFAULT 30000,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

-- Create indexes for better query performance
CREATE INDEX idx_connection_name ON connections(name);
CREATE INDEX idx_connection_active ON connections(active);
CREATE INDEX idx_connection_created ON connections(created_at);

-- Add constraints
ALTER TABLE connections ADD CONSTRAINT chk_security_protocol 
    CHECK (security_protocol IN ('PLAINTEXT', 'SSL', 'SASL_PLAINTEXT', 'SASL_SSL'));

ALTER TABLE connections ADD CONSTRAINT chk_sasl_mechanism 
    CHECK (sasl_mechanism IS NULL OR sasl_mechanism IN ('PLAIN', 'SCRAM-SHA-256', 'SCRAM-SHA-512', 'GSSAPI', 'OAUTHBEARER', 'AWS_MSK_IAM'));

-- Add unique constraint on connection name
ALTER TABLE connections ADD CONSTRAINT uk_connection_name UNIQUE (name);

-- Add comments for documentation
COMMENT ON TABLE connections IS 'Stores Kafka cluster connection configurations';
COMMENT ON COLUMN connections.name IS 'Unique name for the connection';
COMMENT ON COLUMN connections.bootstrap_servers IS 'Comma-separated list of Kafka broker addresses';
COMMENT ON COLUMN connections.security_protocol IS 'Security protocol: PLAINTEXT, SSL, SASL_PLAINTEXT, or SASL_SSL';
COMMENT ON COLUMN connections.sasl_mechanism IS 'SASL authentication mechanism when using SASL protocols';
COMMENT ON COLUMN connections.active IS 'Whether this connection is active and available for use';
