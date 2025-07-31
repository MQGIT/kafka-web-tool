-- Create audit_log table for tracking user actions and system events
-- This table provides comprehensive audit trail for security and compliance

CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(100),
    action VARCHAR(50) NOT NULL,
    user_id VARCHAR(100),
    username VARCHAR(100),
    ip_address INET,
    user_agent VARCHAR(500),
    session_id VARCHAR(100),
    details JSONB,
    old_values JSONB,
    new_values JSONB,
    success BOOLEAN NOT NULL DEFAULT true,
    error_message VARCHAR(1000),
    duration_ms INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX idx_audit_event_type ON audit_log(event_type);
CREATE INDEX idx_audit_entity_type ON audit_log(entity_type);
CREATE INDEX idx_audit_entity_id ON audit_log(entity_id);
CREATE INDEX idx_audit_action ON audit_log(action);
CREATE INDEX idx_audit_user_id ON audit_log(user_id);
CREATE INDEX idx_audit_username ON audit_log(username);
CREATE INDEX idx_audit_created_at ON audit_log(created_at);
CREATE INDEX idx_audit_success ON audit_log(success);

-- Create composite indexes for common queries
CREATE INDEX idx_audit_user_action ON audit_log(user_id, action, created_at);
CREATE INDEX idx_audit_entity_action ON audit_log(entity_type, entity_id, action);

-- Add constraints
ALTER TABLE audit_log ADD CONSTRAINT chk_event_type 
    CHECK (event_type IN ('CONNECTION', 'PRODUCER', 'CONSUMER', 'TOPIC', 'AUTHENTICATION', 'AUTHORIZATION', 'SYSTEM'));

ALTER TABLE audit_log ADD CONSTRAINT chk_entity_type 
    CHECK (entity_type IN ('CONNECTION', 'CONSUMER_SESSION', 'MESSAGE', 'TOPIC', 'USER', 'SYSTEM'));

ALTER TABLE audit_log ADD CONSTRAINT chk_action 
    CHECK (action IN ('CREATE', 'READ', 'UPDATE', 'DELETE', 'START', 'STOP', 'PAUSE', 'RESUME', 'SEND', 'CONSUME', 'LOGIN', 'LOGOUT', 'ACCESS_DENIED'));

-- Add comments for documentation
COMMENT ON TABLE audit_log IS 'Comprehensive audit trail for all user actions and system events';
COMMENT ON COLUMN audit_log.event_type IS 'Type of event being audited';
COMMENT ON COLUMN audit_log.entity_type IS 'Type of entity being acted upon';
COMMENT ON COLUMN audit_log.entity_id IS 'Identifier of the specific entity';
COMMENT ON COLUMN audit_log.action IS 'Action performed on the entity';
COMMENT ON COLUMN audit_log.user_id IS 'Unique identifier of the user performing the action';
COMMENT ON COLUMN audit_log.username IS 'Username of the user performing the action';
COMMENT ON COLUMN audit_log.ip_address IS 'IP address of the client';
COMMENT ON COLUMN audit_log.user_agent IS 'User agent string from the client';
COMMENT ON COLUMN audit_log.session_id IS 'Session identifier';
COMMENT ON COLUMN audit_log.details IS 'Additional details about the event in JSON format';
COMMENT ON COLUMN audit_log.old_values IS 'Previous values before the change (for UPDATE actions)';
COMMENT ON COLUMN audit_log.new_values IS 'New values after the change (for CREATE/UPDATE actions)';
COMMENT ON COLUMN audit_log.success IS 'Whether the action was successful';
COMMENT ON COLUMN audit_log.error_message IS 'Error message if the action failed';
COMMENT ON COLUMN audit_log.duration_ms IS 'Duration of the action in milliseconds';
