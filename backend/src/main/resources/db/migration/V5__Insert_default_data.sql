-- Insert default data for initial application setup
-- This includes a default admin user and sample connection

-- Insert default admin user (password: admin123 - should be changed in production)
-- Password hash is bcrypt hash of 'admin123'
INSERT INTO users (username, email, password_hash, first_name, last_name, role, active, email_verified, created_by)
VALUES (
    'admin',
    'admin@kafkatool.marsem.org',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTUlaGEHX/E4q6YzLMZeA9tL4eMNZbSa',
    'System',
    'Administrator',
    'ADMIN',
    true,
    true,
    'system'
);

-- Production-ready database - no default sample data
-- Users should create their own connections through the UI

-- Grant admin user full permissions on all connections
INSERT INTO user_permissions (user_id, connection_id, permission, granted_by)
SELECT 
    u.id,
    c.id,
    'ADMIN',
    u.id
FROM users u, connections c
WHERE u.username = 'admin';

-- Insert audit log entry for initial setup
INSERT INTO audit_log (
    event_type,
    entity_type,
    action,
    user_id,
    username,
    details,
    success
) VALUES (
    'SYSTEM',
    'SYSTEM',
    'CREATE',
    'system',
    'system',
    '{"message": "Initial database setup completed", "version": "2.0.0"}',
    true
);

-- Create a function to update the updated_at timestamp automatically
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers to automatically update updated_at columns
CREATE TRIGGER update_connections_updated_at 
    BEFORE UPDATE ON connections 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_consumer_sessions_updated_at 
    BEFORE UPDATE ON consumer_sessions 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_users_updated_at 
    BEFORE UPDATE ON users 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create a function to clean up expired sessions
CREATE OR REPLACE FUNCTION cleanup_expired_sessions()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM user_sessions 
    WHERE expires_at < CURRENT_TIMESTAMP OR active = false;
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    
    -- Log the cleanup
    INSERT INTO audit_log (
        event_type,
        entity_type,
        action,
        user_id,
        username,
        details,
        success
    ) VALUES (
        'SYSTEM',
        'USER_SESSION',
        'DELETE',
        'system',
        'system',
        json_build_object('deleted_sessions', deleted_count),
        true
    );
    
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Create a function to clean up old audit logs (keep last 90 days)
CREATE OR REPLACE FUNCTION cleanup_old_audit_logs()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM audit_log 
    WHERE created_at < CURRENT_TIMESTAMP - INTERVAL '90 days';
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;
