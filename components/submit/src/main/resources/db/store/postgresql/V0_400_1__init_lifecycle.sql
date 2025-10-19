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
    error_message           text,
    update_datetime         timestamp default current_timestamp
);

CREATE INDEX idx_submitted_tx_status 
    ON submitted_transaction(status);