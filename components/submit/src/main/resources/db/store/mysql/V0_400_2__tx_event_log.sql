-- Transaction Event Log (Audit Trail)
-- Records all status transitions for debugging and analytics

CREATE TABLE tx_event_log
(
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    tx_hash                 VARCHAR(64) NOT NULL,
    previous_status         VARCHAR(20),
    new_status              VARCHAR(20) NOT NULL,
    message                 TEXT,
    event_timestamp         TIMESTAMP NOT NULL,
    confirmed_slot          BIGINT,
    confirmed_block_number  BIGINT,
    update_datetime         TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

