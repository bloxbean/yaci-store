drop table if exists analytics_export_state;
create table analytics_export_state (
    table_name        VARCHAR(100)  NOT NULL,
    partition_value   VARCHAR(50)   NOT NULL,
    export_status     VARCHAR(20)   NOT NULL,
    row_count         BIGINT,
    file_size_bytes   BIGINT,
    file_path         VARCHAR(500),
    checksum_sha256   VARCHAR(64),
    started_at        TIMESTAMP,
    completed_at      TIMESTAMP,
    duration_seconds  INTEGER,
    error_message     TEXT,
    retry_count       INTEGER       DEFAULT 0,
    created_at        TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (table_name, partition_value)
);

CREATE INDEX idx_analytics_export_status ON analytics_export_state(table_name, export_status);
CREATE INDEX idx_analytics_export_completed ON analytics_export_state(completed_at DESC);
