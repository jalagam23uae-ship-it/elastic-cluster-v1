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

-- Account status enumeration
CREATE TYPE account_status AS ENUM (
    'ACTIVE', 'BLOCKED', 'CLOSED', 'SUSPENDED', 'DORMANT'
);

-- Account type enumeration
CREATE TYPE account_type AS ENUM (
    'CHECKING', 'SAVINGS', 'LOAN', 'INVESTMENT', 'CREDIT_CARD', 'MONEY_MARKET'
);

-- Mandate status enumeration
CREATE TYPE mandate_status AS ENUM (
    'ACTIVE', 'SUSPENDED', 'REVOKED', 'EXPIRED', 'PENDING'
);

-- Reversal status enumeration
CREATE TYPE reversal_status AS ENUM (
    'REQUESTED', 'ACCEPTED', 'SETTLED', 'REJECTED', 'EXPIRED'
);

-- Investigation status enumeration
CREATE TYPE investigation_status AS ENUM (
    'OPEN', 'PENDING', 'RESOLVED', 'CLOSED', 'ESCALATED'
);

-- RFP status enumeration
CREATE TYPE rfp_status AS ENUM (
    'REQUESTED', 'ACCEPTED', 'REJECTED', 'CANCELLED', 'EXPIRED'
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

-- ========================================
-- CRITICAL MISSING TABLES
-- ========================================

-- Accounts table - stores customer and institutional accounts
CREATE TABLE accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_number VARCHAR(34) NOT NULL UNIQUE,  -- IBAN format
    account_type account_type NOT NULL,
    account_status account_status NOT NULL DEFAULT 'ACTIVE',
    account_holder_name VARCHAR(140) NOT NULL,
    account_holder_id VARCHAR(35),  -- Tax ID or customer ID
    institution_bic VARCHAR(11) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    balance DECIMAL(18, 5) NOT NULL DEFAULT 0,
    available_balance DECIMAL(18, 5) NOT NULL DEFAULT 0,
    overdraft_limit DECIMAL(18, 5) DEFAULT 0,
    opened_date DATE NOT NULL DEFAULT CURRENT_DATE,
    closed_date DATE,
    last_transaction_date TIMESTAMP WITH TIME ZONE,
    ofac_status VARCHAR(20) DEFAULT 'CLEAR',
    ofac_last_check TIMESTAMP WITH TIME ZONE,
    fraud_score INTEGER DEFAULT 0 CHECK (fraud_score BETWEEN 0 AND 100),
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT accounts_balance_check CHECK (balance >= -overdraft_limit),
    CONSTRAINT accounts_available_balance_check CHECK (available_balance >= -overdraft_limit)
);

-- Indexes on accounts table
CREATE INDEX idx_accounts_account_number ON accounts(account_number);
CREATE INDEX idx_accounts_institution_bic ON accounts(institution_bic);
CREATE INDEX idx_accounts_account_holder_name ON accounts(account_holder_name);
CREATE INDEX idx_accounts_account_status ON accounts(account_status);
CREATE INDEX idx_accounts_ofac_status ON accounts(ofac_status);
CREATE INDEX idx_accounts_metadata_gin ON accounts USING gin(metadata);

-- BIC directory table - stores bank identification codes and routing info
CREATE TABLE bic_directory (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    bic VARCHAR(11) NOT NULL UNIQUE,  -- 8 or 11 character BIC
    institution_name VARCHAR(140) NOT NULL,
    branch_name VARCHAR(140),
    address_line1 VARCHAR(70),
    address_line2 VARCHAR(70),
    city VARCHAR(35),
    state_province VARCHAR(35),
    postal_code VARCHAR(16),
    country_code VARCHAR(2) NOT NULL,  -- ISO 3166-1 alpha-2
    swift_enabled BOOLEAN NOT NULL DEFAULT true,
    fednow_participant BOOLEAN NOT NULL DEFAULT false,
    routing_number VARCHAR(9),  -- ABA routing number (US only)
    active BOOLEAN NOT NULL DEFAULT true,
    activation_date DATE,
    deactivation_date DATE,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes on bic_directory table
CREATE INDEX idx_bic_directory_bic ON bic_directory(bic);
CREATE INDEX idx_bic_directory_institution_name ON bic_directory(institution_name);
CREATE INDEX idx_bic_directory_routing_number ON bic_directory(routing_number) WHERE routing_number IS NOT NULL;
CREATE INDEX idx_bic_directory_fednow_participant ON bic_directory(fednow_participant) WHERE fednow_participant = true;
CREATE INDEX idx_bic_directory_country_code ON bic_directory(country_code);
CREATE INDEX idx_bic_directory_active ON bic_directory(active) WHERE active = true;

-- Mandates table - stores direct debit mandates
CREATE TABLE mandates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    mandate_id VARCHAR(35) NOT NULL UNIQUE,
    mandate_status mandate_status NOT NULL DEFAULT 'PENDING',
    creditor_name VARCHAR(140) NOT NULL,
    creditor_account VARCHAR(34) NOT NULL,
    creditor_bic VARCHAR(11) NOT NULL,
    debtor_name VARCHAR(140) NOT NULL,
    debtor_account VARCHAR(34) NOT NULL,
    debtor_bic VARCHAR(11) NOT NULL,
    mandate_type VARCHAR(20) NOT NULL CHECK (mandate_type IN ('RCUR', 'OOFF', 'FNAL', 'FRST')),
    max_amount DECIMAL(18, 5),
    frequency VARCHAR(20),  -- e.g., 'MONTHLY', 'WEEKLY', 'DAILY'
    signature_date DATE NOT NULL,
    activation_date DATE,
    expiration_date DATE,
    last_used_date DATE,
    usage_count INTEGER NOT NULL DEFAULT 0,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT mandates_max_amount_check CHECK (max_amount IS NULL OR max_amount > 0)
);

-- Indexes on mandates table
CREATE INDEX idx_mandates_mandate_id ON mandates(mandate_id);
CREATE INDEX idx_mandates_mandate_status ON mandates(mandate_status);
CREATE INDEX idx_mandates_debtor_account ON mandates(debtor_account);
CREATE INDEX idx_mandates_creditor_account ON mandates(creditor_account);
CREATE INDEX idx_mandates_expiration_date ON mandates(expiration_date) WHERE expiration_date IS NOT NULL;

-- Reversals table - tracks payment reversals (pacs.007)
CREATE TABLE reversals (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    reversal_id VARCHAR(35) NOT NULL UNIQUE,
    original_message_id UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    reversal_message_id UUID REFERENCES messages(id) ON DELETE SET NULL,
    reversal_status reversal_status NOT NULL DEFAULT 'REQUESTED',
    reversal_reason_code VARCHAR(4) NOT NULL,
    reversal_reason_description TEXT,
    original_uetr UUID NOT NULL,
    original_end_to_end_id VARCHAR(35),
    original_amount DECIMAL(18, 5) NOT NULL,
    original_currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    reversal_requested_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reversal_processed_at TIMESTAMP WITH TIME ZONE,
    reversal_deadline TIMESTAMP WITH TIME ZONE NOT NULL,  -- 15 seconds for FedNow
    within_window BOOLEAN NOT NULL DEFAULT true,
    initiated_by VARCHAR(140) NOT NULL,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT reversals_amount_check CHECK (original_amount > 0)
);

-- Indexes on reversals table
CREATE INDEX idx_reversals_reversal_id ON reversals(reversal_id);
CREATE INDEX idx_reversals_original_message_id ON reversals(original_message_id);
CREATE INDEX idx_reversals_original_uetr ON reversals(original_uetr);
CREATE INDEX idx_reversals_reversal_status ON reversals(reversal_status);
CREATE INDEX idx_reversals_reversal_deadline ON reversals(reversal_deadline);
CREATE INDEX idx_reversals_within_window ON reversals(within_window);
CREATE INDEX idx_reversals_created_at ON reversals(created_at DESC);

-- Account transactions table - detailed transaction history
CREATE TABLE account_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    message_id UUID REFERENCES messages(id) ON DELETE SET NULL,
    transaction_type VARCHAR(20) NOT NULL CHECK (transaction_type IN ('DEBIT', 'CREDIT', 'HOLD', 'RELEASE', 'FEE', 'REVERSAL')),
    amount DECIMAL(18, 5) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    balance_before DECIMAL(18, 5) NOT NULL,
    balance_after DECIMAL(18, 5) NOT NULL,
    counterparty_name VARCHAR(140),
    counterparty_account VARCHAR(34),
    counterparty_bic VARCHAR(11),
    end_to_end_id VARCHAR(35),
    transaction_id VARCHAR(35),
    uetr UUID,
    description VARCHAR(500),
    booking_date DATE NOT NULL DEFAULT CURRENT_DATE,
    value_date DATE NOT NULL DEFAULT CURRENT_DATE,
    transaction_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT account_transactions_amount_check CHECK (amount != 0)
);

-- Indexes on account_transactions table
CREATE INDEX idx_account_transactions_account_id ON account_transactions(account_id);
CREATE INDEX idx_account_transactions_message_id ON account_transactions(message_id) WHERE message_id IS NOT NULL;
CREATE INDEX idx_account_transactions_transaction_type ON account_transactions(transaction_type);
CREATE INDEX idx_account_transactions_uetr ON account_transactions(uetr) WHERE uetr IS NOT NULL;
CREATE INDEX idx_account_transactions_end_to_end_id ON account_transactions(end_to_end_id) WHERE end_to_end_id IS NOT NULL;
CREATE INDEX idx_account_transactions_booking_date ON account_transactions(booking_date DESC);
CREATE INDEX idx_account_transactions_transaction_time ON account_transactions(transaction_time DESC);

-- RFP (Request for Payment) requests table - pain.013 / pain.014
CREATE TABLE rfp_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    rfp_id VARCHAR(35) NOT NULL UNIQUE,
    rfp_status rfp_status NOT NULL DEFAULT 'REQUESTED',
    creditor_name VARCHAR(140) NOT NULL,
    creditor_account VARCHAR(34) NOT NULL,
    creditor_bic VARCHAR(11) NOT NULL,
    debtor_name VARCHAR(140) NOT NULL,
    debtor_account VARCHAR(34),
    debtor_bic VARCHAR(11),
    requested_amount DECIMAL(18, 5) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    due_date DATE,
    invoice_number VARCHAR(35),
    invoice_date DATE,
    remittance_information VARCHAR(140),
    request_message_id UUID REFERENCES messages(id) ON DELETE SET NULL,
    response_message_id UUID REFERENCES messages(id) ON DELETE SET NULL,
    related_payment_id UUID REFERENCES messages(id) ON DELETE SET NULL,
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMP WITH TIME ZONE,
    expiration_date TIMESTAMP WITH TIME ZONE,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT rfp_requests_amount_check CHECK (requested_amount > 0)
);

-- Indexes on rfp_requests table
CREATE INDEX idx_rfp_requests_rfp_id ON rfp_requests(rfp_id);
CREATE INDEX idx_rfp_requests_rfp_status ON rfp_requests(rfp_status);
CREATE INDEX idx_rfp_requests_creditor_account ON rfp_requests(creditor_account);
CREATE INDEX idx_rfp_requests_debtor_account ON rfp_requests(debtor_account) WHERE debtor_account IS NOT NULL;
CREATE INDEX idx_rfp_requests_due_date ON rfp_requests(due_date) WHERE due_date IS NOT NULL;
CREATE INDEX idx_rfp_requests_expiration_date ON rfp_requests(expiration_date) WHERE expiration_date IS NOT NULL;

-- Investigations table - payment investigation requests (camt.028 / camt.029)
CREATE TABLE investigations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    investigation_id VARCHAR(35) NOT NULL UNIQUE,
    investigation_status investigation_status NOT NULL DEFAULT 'OPEN',
    investigation_type VARCHAR(50) NOT NULL CHECK (investigation_type IN ('MISSING_PAYMENT', 'DUPLICATE_PAYMENT', 'WRONG_AMOUNT', 'WRONG_BENEFICIARY', 'FRAUDULENT', 'OTHER')),
    original_message_id UUID REFERENCES messages(id) ON DELETE SET NULL,
    original_uetr UUID,
    original_end_to_end_id VARCHAR(35),
    original_amount DECIMAL(18, 5),
    original_currency VARCHAR(3) DEFAULT 'USD',
    requester_bic VARCHAR(11) NOT NULL,
    responder_bic VARCHAR(11) NOT NULL,
    request_message_id UUID REFERENCES messages(id) ON DELETE SET NULL,
    response_message_id UUID REFERENCES messages(id) ON DELETE SET NULL,
    description TEXT NOT NULL,
    resolution TEXT,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    opened_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMP WITH TIME ZONE,
    resolved_at TIMESTAMP WITH TIME ZONE,
    closed_at TIMESTAMP WITH TIME ZONE,
    sla_deadline TIMESTAMP WITH TIME ZONE,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes on investigations table
CREATE INDEX idx_investigations_investigation_id ON investigations(investigation_id);
CREATE INDEX idx_investigations_investigation_status ON investigations(investigation_status);
CREATE INDEX idx_investigations_investigation_type ON investigations(investigation_type);
CREATE INDEX idx_investigations_original_uetr ON investigations(original_uetr) WHERE original_uetr IS NOT NULL;
CREATE INDEX idx_investigations_priority ON investigations(priority);
CREATE INDEX idx_investigations_sla_deadline ON investigations(sla_deadline) WHERE sla_deadline IS NOT NULL;
CREATE INDEX idx_investigations_opened_at ON investigations(opened_at DESC);

-- Triggers for updated_at on new tables
CREATE TRIGGER update_accounts_updated_at
    BEFORE UPDATE ON accounts
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_bic_directory_updated_at
    BEFORE UPDATE ON bic_directory
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_mandates_updated_at
    BEFORE UPDATE ON mandates
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_reversals_updated_at
    BEFORE UPDATE ON reversals
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_rfp_requests_updated_at
    BEFORE UPDATE ON rfp_requests
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_investigations_updated_at
    BEFORE UPDATE ON investigations
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

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
COMMENT ON TABLE accounts IS 'Customer and institutional accounts with OFAC and fraud tracking';
COMMENT ON TABLE bic_directory IS 'Bank identification codes (BIC/SWIFT) and routing information';
COMMENT ON TABLE mandates IS 'Direct debit mandates for recurring payments';
COMMENT ON TABLE reversals IS 'Payment reversals with 15-second FedNow window tracking';
COMMENT ON TABLE account_transactions IS 'Detailed transaction history for all accounts';
COMMENT ON TABLE rfp_requests IS 'Request for Payment (pain.013/pain.014) tracking';
COMMENT ON TABLE investigations IS 'Payment investigation requests (camt.028/camt.029)';

-- Grant permissions (adjust as needed for your environment)
-- GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA public TO fednow_app;
-- GRANT SELECT ON ALL SEQUENCES IN SCHEMA public TO fednow_app;
