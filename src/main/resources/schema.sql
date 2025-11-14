-- ISO 20022 Message Converter Database Schema
-- PostgreSQL 15+

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Message types enumeration
CREATE TYPE message_type AS ENUM (
    'pain_001', 'pain_002', 'pain_007', 'pain_008', 'pain_009', 'pain_013', 'pain_014',
    'pacs_002', 'pacs_003', 'pacs_004', 'pacs_007', 'pacs_008', 'pacs_009', 'pacs_028',
    'camt_028', 'camt_029', 'camt_050', 'camt_052', 'camt_053', 'camt_054', 'camt_056', 'camt_057', 'camt_058',
    'acmt_007', 'acmt_023', 'acmt_024',
    'admi_002', 'admi_007'
);

-- Message status enumeration
CREATE TYPE message_status AS ENUM (
    'RECEIVED', 'VALIDATED', 'CONVERTED', 'SENT', 'ACKNOWLEDGED',
    'ACCEPTED', 'REJECTED', 'PENDING', 'FAILED', 'ERROR'
);

-- Payment status codes
CREATE TYPE payment_status_code AS ENUM (
    'ACCP', 'ACSC', 'ACSP', 'ACTC', 'ACWC',
    'PART', 'PDNG', 'RCVD', 'RJCT', 'CANC'
);

-- Messages table - stores all ISO 20022 messages
CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    message_id VARCHAR(35) NOT NULL UNIQUE,
    message_type message_type NOT NULL,
    uetr UUID,
    end_to_end_id VARCHAR(35),
    transaction_id VARCHAR(35),
    instruction_id VARCHAR(35),
    original_message_id VARCHAR(35),
    status message_status NOT NULL DEFAULT 'RECEIVED',
    direction VARCHAR(10) NOT NULL CHECK (direction IN ('INBOUND', 'OUTBOUND')),
    xml_content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE,
    instg_agent VARCHAR(35),
    instd_agent VARCHAR(35),
    debtor_name VARCHAR(140),
    debtor_account VARCHAR(34),
    creditor_name VARCHAR(140),
    creditor_account VARCHAR(34),
    amount DECIMAL(18, 5),
    currency VARCHAR(3) DEFAULT 'USD',
    settlement_date DATE,
    metadata JSONB,

    -- Indexes for performance
    CONSTRAINT messages_amount_check CHECK (amount >= 0)
);

-- Indexes on messages table
CREATE INDEX idx_messages_message_id ON messages(message_id);
CREATE INDEX idx_messages_uetr ON messages(uetr) WHERE uetr IS NOT NULL;
CREATE INDEX idx_messages_end_to_end_id ON messages(end_to_end_id) WHERE end_to_end_id IS NOT NULL;
CREATE INDEX idx_messages_transaction_id ON messages(transaction_id) WHERE transaction_id IS NOT NULL;
CREATE INDEX idx_messages_original_message_id ON messages(original_message_id) WHERE original_message_id IS NOT NULL;
CREATE INDEX idx_messages_status ON messages(status);
CREATE INDEX idx_messages_message_type ON messages(message_type);
CREATE INDEX idx_messages_created_at ON messages(created_at DESC);
CREATE INDEX idx_messages_settlement_date ON messages(settlement_date) WHERE settlement_date IS NOT NULL;
CREATE INDEX idx_messages_metadata_gin ON messages USING gin(metadata);

-- Conversions table - tracks message conversions
CREATE TABLE conversions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    source_message_id UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    target_message_id UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    converter_name VARCHAR(100) NOT NULL,
    conversion_time_ms INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('SUCCESS', 'FAILED', 'PARTIAL')),
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT conversions_time_check CHECK (conversion_time_ms >= 0)
);

-- Indexes on conversions table
CREATE INDEX idx_conversions_source_message_id ON conversions(source_message_id);
CREATE INDEX idx_conversions_target_message_id ON conversions(target_message_id);
CREATE INDEX idx_conversions_converter_name ON conversions(converter_name);
CREATE INDEX idx_conversions_created_at ON conversions(created_at DESC);

-- Validation errors table
CREATE TABLE validation_errors (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    message_id UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    error_code VARCHAR(20) NOT NULL,
    error_category VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('INFO', 'WARNING', 'ERROR', 'CRITICAL')),
    field_path VARCHAR(500),
    error_description TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes on validation_errors table
CREATE INDEX idx_validation_errors_message_id ON validation_errors(message_id);
CREATE INDEX idx_validation_errors_error_code ON validation_errors(error_code);
CREATE INDEX idx_validation_errors_severity ON validation_errors(severity);
CREATE INDEX idx_validation_errors_created_at ON validation_errors(created_at DESC);

-- Payment status reports table
CREATE TABLE payment_status_reports (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    message_id UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    original_message_id UUID REFERENCES messages(id) ON DELETE SET NULL,
    status_code payment_status_code NOT NULL,
    reason_code VARCHAR(4),
    reason_description TEXT,
    acceptance_datetime TIMESTAMP WITH TIME ZONE,
    clearing_system_reference VARCHAR(35),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes on payment_status_reports table
CREATE INDEX idx_payment_status_message_id ON payment_status_reports(message_id);
CREATE INDEX idx_payment_status_original_message_id ON payment_status_reports(original_message_id) WHERE original_message_id IS NOT NULL;
CREATE INDEX idx_payment_status_status_code ON payment_status_reports(status_code);
CREATE INDEX idx_payment_status_created_at ON payment_status_reports(created_at DESC);

-- System events table
CREATE TABLE system_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_code VARCHAR(10) NOT NULL,
    event_description TEXT NOT NULL,
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('INFO', 'WARNING', 'ERROR', 'FATAL')),
    related_message_id UUID REFERENCES messages(id) ON DELETE SET NULL,
    event_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    event_parameters JSONB,
    additional_info TEXT
);

-- Indexes on system_events table
CREATE INDEX idx_system_events_event_code ON system_events(event_code);
CREATE INDEX idx_system_events_severity ON system_events(severity);
CREATE INDEX idx_system_events_related_message_id ON system_events(related_message_id) WHERE related_message_id IS NOT NULL;
CREATE INDEX idx_system_events_event_time ON system_events(event_time DESC);

-- Audit log table
CREATE TABLE audit_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    message_id UUID REFERENCES messages(id) ON DELETE SET NULL,
    action VARCHAR(50) NOT NULL,
    actor VARCHAR(100),
    details JSONB,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes on audit_log table
CREATE INDEX idx_audit_log_message_id ON audit_log(message_id) WHERE message_id IS NOT NULL;
CREATE INDEX idx_audit_log_action ON audit_log(action);
CREATE INDEX idx_audit_log_timestamp ON audit_log(timestamp DESC);

-- Converter metrics table
CREATE TABLE converter_metrics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    converter_name VARCHAR(100) NOT NULL,
    execution_count BIGINT NOT NULL DEFAULT 0,
    success_count BIGINT NOT NULL DEFAULT 0,
    failure_count BIGINT NOT NULL DEFAULT 0,
    avg_execution_time_ms DECIMAL(10, 2) NOT NULL DEFAULT 0,
    min_execution_time_ms INTEGER NOT NULL DEFAULT 0,
    max_execution_time_ms INTEGER NOT NULL DEFAULT 0,
    last_execution_at TIMESTAMP WITH TIME ZONE,
    date DATE NOT NULL DEFAULT CURRENT_DATE,

    CONSTRAINT converter_metrics_unique UNIQUE (converter_name, date)
);

-- Indexes on converter_metrics table
CREATE INDEX idx_converter_metrics_converter_name ON converter_metrics(converter_name);
CREATE INDEX idx_converter_metrics_date ON converter_metrics(date DESC);

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger to automatically update updated_at
CREATE TRIGGER update_messages_updated_at
    BEFORE UPDATE ON messages
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Function to log audit trail
CREATE OR REPLACE FUNCTION log_message_audit()
RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'INSERT') THEN
        INSERT INTO audit_log (message_id, action, details)
        VALUES (NEW.id, 'MESSAGE_CREATED', jsonb_build_object(
            'message_type', NEW.message_type,
            'message_id', NEW.message_id,
            'status', NEW.status
        ));
        RETURN NEW;
    ELSIF (TG_OP = 'UPDATE') THEN
        IF (OLD.status IS DISTINCT FROM NEW.status) THEN
            INSERT INTO audit_log (message_id, action, details)
            VALUES (NEW.id, 'STATUS_CHANGED', jsonb_build_object(
                'old_status', OLD.status,
                'new_status', NEW.status
            ));
        END IF;
        RETURN NEW;
    END IF;
    RETURN NULL;
END;
$$ language 'plpgsql';

-- Trigger for audit logging
CREATE TRIGGER message_audit_trigger
    AFTER INSERT OR UPDATE ON messages
    FOR EACH ROW
    EXECUTE FUNCTION log_message_audit();

-- Views for common queries

-- Active payments view
CREATE VIEW active_payments AS
SELECT
    m.id,
    m.message_id,
    m.uetr,
    m.end_to_end_id,
    m.status,
    m.debtor_name,
    m.creditor_name,
    m.amount,
    m.currency,
    m.settlement_date,
    m.created_at
FROM messages m
WHERE m.message_type IN ('pain_001', 'pacs_008')
  AND m.status NOT IN ('REJECTED', 'FAILED');

-- Payment status summary view
CREATE VIEW payment_status_summary AS
SELECT
    m.uetr,
    m.end_to_end_id,
    m.message_id,
    m.status,
    psr.status_code,
    psr.reason_code,
    psr.reason_description,
    psr.acceptance_datetime,
    m.amount,
    m.currency,
    m.created_at
FROM messages m
LEFT JOIN payment_status_reports psr ON m.id = psr.message_id
WHERE m.message_type IN ('pacs_002', 'pain_002');

-- Converter performance view
CREATE VIEW converter_performance AS
SELECT
    converter_name,
    SUM(execution_count) as total_executions,
    SUM(success_count) as total_successes,
    SUM(failure_count) as total_failures,
    ROUND(AVG(avg_execution_time_ms), 2) as avg_time_ms,
    MIN(min_execution_time_ms) as min_time_ms,
    MAX(max_execution_time_ms) as max_time_ms,
    ROUND(SUM(success_count)::numeric / NULLIF(SUM(execution_count), 0) * 100, 2) as success_rate_pct
FROM converter_metrics
GROUP BY converter_name;

-- Comments on tables
COMMENT ON TABLE messages IS 'Stores all ISO 20022 messages (pain, pacs, camt, admi)';
COMMENT ON TABLE conversions IS 'Tracks message-to-message conversions';
COMMENT ON TABLE validation_errors IS 'Records validation errors for messages';
COMMENT ON TABLE payment_status_reports IS 'Stores payment status information (ACCP, RJCT, PDNG)';
COMMENT ON TABLE system_events IS 'Records system events and notifications (admi.002)';
COMMENT ON TABLE audit_log IS 'Audit trail for all message operations';
COMMENT ON TABLE converter_metrics IS 'Performance metrics for converters';

-- Grant permissions (adjust as needed for your environment)
-- GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA public TO fednow_app;
-- GRANT SELECT ON ALL SEQUENCES IN SCHEMA public TO fednow_app;
