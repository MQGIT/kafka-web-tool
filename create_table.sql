CREATE TABLE consumer_messages (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(100) NOT NULL,
    topic VARCHAR(255) NOT NULL,
    partition INTEGER NOT NULL,
    message_offset BIGINT NOT NULL,
    message_key TEXT,
    message_value TEXT,
    message_timestamp TIMESTAMP,
    serialized_key_size INTEGER,
    serialized_value_size INTEGER,
    leader_epoch INTEGER,
    consumer_group VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_consumer_message_session ON consumer_messages(session_id);
CREATE INDEX idx_consumer_message_topic ON consumer_messages(topic);
CREATE INDEX idx_consumer_message_offset ON consumer_messages(topic, partition, message_offset);
CREATE INDEX idx_consumer_message_created ON consumer_messages(created_at);

-- Add foreign key constraint
ALTER TABLE consumer_messages 
    ADD CONSTRAINT fk_consumer_messages_session 
    FOREIGN KEY (session_id) REFERENCES consumer_sessions(session_id) ON DELETE CASCADE;
