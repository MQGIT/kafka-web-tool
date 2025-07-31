-- Create users table for application user management
-- This table stores user information for authentication and authorization

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    active BOOLEAN NOT NULL DEFAULT true,
    email_verified BOOLEAN NOT NULL DEFAULT false,
    last_login_at TIMESTAMP,
    last_login_ip INET,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMP,
    password_changed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

-- Create user_sessions table for session management
CREATE TABLE user_sessions (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    ip_address INET,
    user_agent VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_accessed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true
);

-- Create user_permissions table for fine-grained permissions
CREATE TABLE user_permissions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    connection_id BIGINT,
    permission VARCHAR(50) NOT NULL,
    granted_by BIGINT,
    granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_active ON users(active);
CREATE INDEX idx_users_last_login ON users(last_login_at);

CREATE INDEX idx_user_sessions_session_id ON user_sessions(session_id);
CREATE INDEX idx_user_sessions_user_id ON user_sessions(user_id);
CREATE INDEX idx_user_sessions_active ON user_sessions(active);
CREATE INDEX idx_user_sessions_expires ON user_sessions(expires_at);

CREATE INDEX idx_user_permissions_user_id ON user_permissions(user_id);
CREATE INDEX idx_user_permissions_connection_id ON user_permissions(connection_id);
CREATE INDEX idx_user_permissions_permission ON user_permissions(permission);

-- Add foreign key constraints
ALTER TABLE user_sessions 
    ADD CONSTRAINT fk_user_sessions_user 
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE user_permissions 
    ADD CONSTRAINT fk_user_permissions_user 
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE user_permissions 
    ADD CONSTRAINT fk_user_permissions_connection 
    FOREIGN KEY (connection_id) REFERENCES connections(id) ON DELETE CASCADE;

ALTER TABLE user_permissions 
    ADD CONSTRAINT fk_user_permissions_granted_by 
    FOREIGN KEY (granted_by) REFERENCES users(id) ON DELETE SET NULL;

-- Add constraints
ALTER TABLE users ADD CONSTRAINT chk_user_role 
    CHECK (role IN ('ADMIN', 'MANAGER', 'USER', 'READONLY'));

ALTER TABLE users ADD CONSTRAINT chk_failed_login_attempts 
    CHECK (failed_login_attempts >= 0);

ALTER TABLE user_permissions ADD CONSTRAINT chk_user_permission 
    CHECK (permission IN ('READ', 'WRITE', 'ADMIN', 'PRODUCE', 'CONSUME', 'MANAGE_TOPICS', 'MANAGE_CONSUMERS'));

-- Add unique constraint for user permissions
ALTER TABLE user_permissions 
    ADD CONSTRAINT uk_user_connection_permission 
    UNIQUE (user_id, connection_id, permission);

-- Add comments for documentation
COMMENT ON TABLE users IS 'Application users with authentication and authorization information';
COMMENT ON COLUMN users.username IS 'Unique username for login';
COMMENT ON COLUMN users.email IS 'User email address (must be unique)';
COMMENT ON COLUMN users.password_hash IS 'Hashed password (null for external authentication)';
COMMENT ON COLUMN users.role IS 'User role determining base permissions';
COMMENT ON COLUMN users.active IS 'Whether the user account is active';
COMMENT ON COLUMN users.email_verified IS 'Whether the email address has been verified';
COMMENT ON COLUMN users.failed_login_attempts IS 'Number of consecutive failed login attempts';
COMMENT ON COLUMN users.locked_until IS 'Account locked until this timestamp (null if not locked)';

COMMENT ON TABLE user_sessions IS 'Active user sessions for session management';
COMMENT ON COLUMN user_sessions.session_id IS 'Unique session identifier';
COMMENT ON COLUMN user_sessions.expires_at IS 'When this session expires';

COMMENT ON TABLE user_permissions IS 'Fine-grained permissions for users on specific connections';
COMMENT ON COLUMN user_permissions.connection_id IS 'Specific connection (null for global permissions)';
COMMENT ON COLUMN user_permissions.permission IS 'Type of permission granted';
COMMENT ON COLUMN user_permissions.granted_by IS 'User who granted this permission';
COMMENT ON COLUMN user_permissions.expires_at IS 'When this permission expires (null for permanent)';
