-- Create consumer_messages table for storing consumed Kafka messages
-- This table stores the actual consumed messages for retrieval and display

CREATE TABLE consumer_messages (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(100) NOT NULL,
    topic VARCHAR(255) NOT NULL,
    partition INTEGER NOT NULL,
    offset BIGINT NOT NULL,
    message_key TEXT,
    message_value TEXT,
    message_timestamp TIMESTAMP,
    serialized_key_size INTEGER,
    serialized_value_size INTEGER,
    leader_epoch INTEGER,
    consumer_group VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX idx_consumer_message_session ON consumer_messages(session_id);
CREATE INDEX idx_consumer_message_topic ON consumer_messages(topic);
CREATE INDEX idx_consumer_message_offset ON consumer_messages(topic, partition, offset);
CREATE INDEX idx_consumer_message_created ON consumer_messages(created_at);
CREATE INDEX idx_consumer_message_session_created ON consumer_messages(session_id, created_at);
CREATE INDEX idx_consumer_message_session_topic ON consumer_messages(session_id, topic);

-- Create unique constraint to prevent duplicate messages
CREATE UNIQUE INDEX idx_consumer_message_unique ON consumer_messages(session_id, topic, partition, offset);

-- Add foreign key constraint to consumer_sessions table
ALTER TABLE consumer_messages 
    ADD CONSTRAINT fk_consumer_messages_session 
    FOREIGN KEY (session_id) REFERENCES consumer_sessions(session_id) ON DELETE CASCADE;

-- Add constraints
ALTER TABLE consumer_messages ADD CONSTRAINT chk_partition_positive 
    CHECK (partition >= 0);

ALTER TABLE consumer_messages ADD CONSTRAINT chk_offset_positive 
    CHECK (offset >= 0);

ALTER TABLE consumer_messages ADD CONSTRAINT chk_key_size_positive 
    CHECK (serialized_key_size IS NULL OR serialized_key_size >= 0);

ALTER TABLE consumer_messages ADD CONSTRAINT chk_value_size_positive 
    CHECK (serialized_value_size IS NULL OR serialized_value_size >= 0);

-- Add comments for documentation
COMMENT ON TABLE consumer_messages IS 'Stores consumed Kafka messages for consumer sessions';
COMMENT ON COLUMN consumer_messages.session_id IS 'Reference to the consumer session';
COMMENT ON COLUMN consumer_messages.topic IS 'Kafka topic name';
COMMENT ON COLUMN consumer_messages.partition IS 'Kafka partition number';
COMMENT ON COLUMN consumer_messages.offset IS 'Kafka message offset';
COMMENT ON COLUMN consumer_messages.message_key IS 'Kafka message key';
COMMENT ON COLUMN consumer_messages.message_value IS 'Kafka message value';
COMMENT ON COLUMN consumer_messages.message_timestamp IS 'Original Kafka message timestamp';
COMMENT ON COLUMN consumer_messages.serialized_key_size IS 'Size of serialized key in bytes';
COMMENT ON COLUMN consumer_messages.serialized_value_size IS 'Size of serialized value in bytes';
COMMENT ON COLUMN consumer_messages.leader_epoch IS 'Kafka leader epoch';
COMMENT ON COLUMN consumer_messages.consumer_group IS 'Kafka consumer group';
COMMENT ON COLUMN consumer_messages.created_at IS 'When the message was stored in the database';
