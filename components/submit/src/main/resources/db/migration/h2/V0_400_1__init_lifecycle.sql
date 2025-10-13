-- Smart Transaction Submission with Lifecycle Tracking
-- ADR-001: submitted_transaction table

drop table if exists submitted_transaction;
create table submitted_transaction
(
    tx_hash                 varchar(64) not null primary key,
    status                  varchar(20) not null,
    -- Values: SUBMITTED, CONFIRMED, SUCCESS, FINALIZED, FAILED, ROLLED_BACK
    
    submitted_at            timestamp not null,
    
    -- Confirmation tracking
    confirmed_at            timestamp,
    confirmed_slot          bigint,
    confirmed_block_number  bigint,
    
    -- Success and finalized tracking
    success_at              timestamp,
    finalized_at            timestamp,
    
    -- Error tracking
    error_message           clob,
    
    update_datetime         timestamp default current_timestamp
);

-- Indexes for performance
CREATE INDEX idx_submitted_tx_status 
    ON submitted_transaction(status);
    
CREATE INDEX idx_submitted_tx_submitted_at 
    ON submitted_transaction(submitted_at);
    
CREATE INDEX idx_submitted_tx_confirmed_slot 
    ON submitted_transaction(confirmed_slot);
    
CREATE INDEX idx_submitted_tx_confirmed_block 
    ON submitted_transaction(confirmed_block_number);

-- Index for efficient FINALIZED transition query (H2 doesn't support WHERE in index)
CREATE INDEX idx_submitted_tx_success_eligible 
    ON submitted_transaction(status, confirmed_block_number);

