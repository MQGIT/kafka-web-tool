-- Create consumer_sessions table for tracking active Kafka consumer sessions
-- This table manages consumer sessions created through the web application

CREATE TABLE consumer_sessions (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(100) NOT NULL UNIQUE,
    connection_id BIGINT NOT NULL,
    topic VARCHAR(255) NOT NULL,
    consumer_group VARCHAR(255) NOT NULL,
    partition_id INTEGER,
    start_offset BIGINT,
    current_offset BIGINT,
    max_messages INTEGER,
    messages_consumed BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    websocket_session_id VARCHAR(100),
    auto_commit BOOLEAN NOT NULL DEFAULT true,
    poll_timeout_ms INTEGER DEFAULT 1000,
    error_message VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP,
    stopped_at TIMESTAMP,
    created_by VARCHAR(100)
);

-- Create indexes for better query performance
CREATE INDEX idx_consumer_session_id ON consumer_sessions(session_id);
CREATE INDEX idx_consumer_connection ON consumer_sessions(connection_id);
CREATE INDEX idx_consumer_status ON consumer_sessions(status);
CREATE INDEX idx_consumer_created ON consumer_sessions(created_at);
CREATE INDEX idx_consumer_topic ON consumer_sessions(topic);
CREATE INDEX idx_consumer_group ON consumer_sessions(consumer_group);

-- Add foreign key constraint to connections table
ALTER TABLE consumer_sessions 
    ADD CONSTRAINT fk_consumer_sessions_connection 
    FOREIGN KEY (connection_id) REFERENCES connections(id) ON DELETE CASCADE;

-- Add constraints
ALTER TABLE consumer_sessions ADD CONSTRAINT chk_consumer_status 
    CHECK (status IN ('CREATED', 'RUNNING', 'PAUSED', 'STOPPED', 'ERROR'));

ALTER TABLE consumer_sessions ADD CONSTRAINT chk_messages_consumed_positive 
    CHECK (messages_consumed >= 0);

ALTER TABLE consumer_sessions ADD CONSTRAINT chk_max_messages_positive 
    CHECK (max_messages IS NULL OR max_messages > 0);

ALTER TABLE consumer_sessions ADD CONSTRAINT chk_poll_timeout_positive 
    CHECK (poll_timeout_ms > 0);

-- Add comments for documentation
COMMENT ON TABLE consumer_sessions IS 'Tracks active Kafka consumer sessions';
COMMENT ON COLUMN consumer_sessions.session_id IS 'Unique identifier for the consumer session';
COMMENT ON COLUMN consumer_sessions.connection_id IS 'Reference to the Kafka connection';
COMMENT ON COLUMN consumer_sessions.topic IS 'Kafka topic being consumed';
COMMENT ON COLUMN consumer_sessions.consumer_group IS 'Kafka consumer group ID';
COMMENT ON COLUMN consumer_sessions.partition_id IS 'Specific partition to consume from (null for all partitions)';
COMMENT ON COLUMN consumer_sessions.start_offset IS 'Starting offset for consumption';
COMMENT ON COLUMN consumer_sessions.current_offset IS 'Current offset position';
COMMENT ON COLUMN consumer_sessions.max_messages IS 'Maximum number of messages to consume (null for unlimited)';
COMMENT ON COLUMN consumer_sessions.messages_consumed IS 'Number of messages consumed so far';
COMMENT ON COLUMN consumer_sessions.status IS 'Current status of the consumer session';
COMMENT ON COLUMN consumer_sessions.websocket_session_id IS 'WebSocket session ID for real-time streaming';
COMMENT ON COLUMN consumer_sessions.auto_commit IS 'Whether to auto-commit offsets';
COMMENT ON COLUMN consumer_sessions.poll_timeout_ms IS 'Timeout for polling messages in milliseconds';
