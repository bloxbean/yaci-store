set search_path TO dev;

SELECT create_hypertable('address_balance', 'slot', chunk_time_interval => 500000);

CREATE INDEX idx_address_slot ON address_balance (address, slot DESC);

WITH latest_balances AS (
    SELECT DISTINCT ON (address) address, quantity, slot
    FROM address_balance
    WHERE unit = 'lovelace'
    ORDER BY address, slot DESC
)
SELECT address, quantity
FROM latest_balances
ORDER BY quantity DESC
LIMIT 100;

WITH latest_balances AS (
    SELECT DISTINCT ON (address) address, quantity, slot
    FROM address_balance
    ORDER BY address, slot DESC
),
     historic_balances AS (
         SELECT address,
                quantity AS balance_1d,
                LAG(quantity) OVER (PARTITION BY address ORDER BY slot) AS balance_7d,
                LAG(quantity, 30) OVER (PARTITION BY address ORDER BY slot) AS balance_1m,
                LAG(quantity, 90) OVER (PARTITION BY address ORDER BY slot) AS balance_3m,
                LAG(quantity, 180) OVER (PARTITION BY address ORDER BY slot) AS balance_6m
         FROM address_balance
     )
SELECT lb.address,
       lb.quantity AS latest_balance,
       hb.balance_1d,
       lb.quantity - hb.balance_1d AS change_1d,
       hb.balance_7d,
       lb.quantity - hb.balance_7d AS change_7d,
       hb.balance_1m,
       lb.quantity - hb.balance_1m AS change_1m,
       hb.balance_3m,
       lb.quantity - hb.balance_3m AS change_3m,
       hb.balance_6m,
       lb.quantity - hb.balance_6m AS change_6m
FROM latest_balances lb
         LEFT JOIN historic_balances hb ON lb.address = hb.address;



ALTER TABLE address_balance ADD COLUMN block_timestamp TIMESTAMPTZ;

UPDATE address_balance
SET block_timestamp = TO_TIMESTAMP(block_time);

ALTER TABLE dev.address_balance
    ADD COLUMN block_timestamp TIMESTAMPTZ
        GENERATED ALWAYS AS (TO_TIMESTAMP(block_time)) STORED;


ALTER TABLE address_balance
    ALTER COLUMN block_timestamp TYPE TIMESTAMPTZ
        USING block_timestamp AT TIME ZONE 'UTC';

-- Convert to Hypertable
SELECT public.create_hypertable('address_balance', 'slot', migrate_data => true, chunk_time_interval => 432000::BIGINT);


SELECT public.create_hypertable('address_balance', 'block_timestamp', migrate_data => true, chunk_time_interval => INTERVAL '5 days');


-- initialize
ALTER TABLE dev.address_balance ADD COLUMN block_timestamp TIMESTAMPTZ;
UPDATE dev.address_balance SET block_timestamp = TO_TIMESTAMP(block_time);

CREATE OR REPLACE FUNCTION set_block_timestamp()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.block_timestamp = TO_TIMESTAMP(NEW.block_time);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_set_block_timestamp
    BEFORE INSERT ON dev.address_balance
    FOR EACH ROW EXECUTE FUNCTION set_block_timestamp();


WITH latest_balances AS (
    -- Get the latest balance per address
    SELECT DISTINCT ON (address) address, quantity AS latest_balance, block_timestamp
    FROM address_balance
    WHERE unit = 'lovelace'
    ORDER BY address, block_timestamp DESC
),
     historic_balances AS (
         -- Get balance changes at different time intervals
         SELECT address,
                quantity AS latest_balance,
                LAG(quantity) OVER (PARTITION BY address ORDER BY block_timestamp RANGE INTERVAL '1 day' PRECEDING) AS balance_1d,
                LAG(quantity) OVER (PARTITION BY address ORDER BY block_timestamp RANGE INTERVAL '7 days' PRECEDING) AS balance_7d,
                LAG(quantity) OVER (PARTITION BY address ORDER BY block_timestamp RANGE INTERVAL '1 month' PRECEDING) AS balance_1m,
                LAG(quantity) OVER (PARTITION BY address ORDER BY block_timestamp RANGE INTERVAL '3 months' PRECEDING) AS balance_3m,
                LAG(quantity) OVER (PARTITION BY address ORDER BY block_timestamp RANGE INTERVAL '6 months' PRECEDING) AS balance_6m
         FROM address_balance
         WHERE unit = 'lovelace'
     )
SELECT lb.address,
       lb.latest_balance,
       hb.balance_1d,
       lb.latest_balance - hb.balance_1d AS change_1d,
       hb.balance_7d,
       lb.latest_balance - hb.balance_7d AS change_7d,
       hb.balance_1m,
       lb.latest_balance - hb.balance_1m AS change_1m,
       hb.balance_3m,
       lb.latest_balance - hb.balance_3m AS change_3m,
       hb.balance_6m,
       lb.latest_balance - hb.balance_6m AS change_6m
FROM latest_balances lb
         LEFT JOIN historic_balances hb ON lb.address = hb.address
ORDER BY lb.latest_balance DESC
LIMIT 100;



CREATE INDEX idx_address_block_timestamp ON address_balance (address, block_timestamp DESC);

CREATE INDEX idx_address_balance_addr_unit_slot ON address_balance (address, unit, slot DESC);


WITH latest_balances AS (
    SELECT DISTINCT ON (address) address, quantity, slot
    FROM address_balance
    WHERE unit = 'lovelace'
    ORDER BY address, block_timestamp DESC
)
SELECT address, quantity
FROM latest_balances
ORDER BY quantity DESC
LIMIT 100;


-- Final Query to Create

SELECT create_hypertable('address_balance', 'slot', migrate_data => true, chunk_time_interval => 432000);

-- CREATE INDEX

CREATE INDEX idx_address_unit_slot_desc ON address_balance (address, unit, slot DESC);


-- Top 100 Addresses
--
WITH latest_balances AS (
    SELECT DISTINCT ON (address) address, quantity, slot
    FROM address_balance
    WHERE unit = 'lovelace'
    ORDER BY address, slot DESC
)
SELECT address, quantity
FROM latest_balances
ORDER BY quantity DESC
LIMIT 100;

WITH latest_balance AS (
    SELECT address, quantity
    FROM address_balance
    WHERE unit = 'lovelace'
      AND slot = (
        SELECT MAX(slot) FROM address_balance AS sub
        WHERE sub.address = address_balance.address
          AND sub.unit = 'lovelace'
    )
)
SELECT address, SUM(quantity) AS total_balance
FROM latest_balance
GROUP BY address
ORDER BY quantity DESC
LIMIT 100;


CREATE MATERIALIZED VIEW top100_address AS
WITH latest_balances AS (
    SELECT DISTINCT ON (address) address, quantity, slot
    FROM address_balance
    WHERE unit = 'lovelace'
    ORDER BY address, slot DESC
),
current_slot AS (
    SELECT MAX(slot) AS current_slot FROM address_balance
)
SELECT 
    lb.address, 
    lb.quantity,
    cs.current_slot
FROM latest_balances lb, current_slot cs
ORDER BY lb.quantity DESC
LIMIT 100;

-- Refresh

crontab -e
0 */2 * * * psql -U your_username -d your_database -c "REFRESH MATERIALIZED VIEW top100_address;"

-- Find balance at different time windows for top 100

CREATE MATERIALIZED VIEW address_balance_changes AS
WITH balance_changes AS (
    SELECT 
        t.address,
        t.quantity AS current_balance,
        t.current_slot,  -- Using current_slot instead of slot
        
        -- Change over the last 1 day (86400 seconds)
        COALESCE(
            (
                SELECT b.quantity - t.quantity
                FROM address_balance b
                WHERE b.address = t.address 
                  AND b.unit = 'lovelace'
                  AND b.slot = (
                      SELECT MAX(b2.slot)
                      FROM address_balance b2
                      WHERE b2.address = b.address 
                        AND b2.unit = 'lovelace' 
                        AND b2.slot <= (t.current_slot - 86400) -- 1 day ago
                  )
                LIMIT 1
            ), 0) AS change_1d,
        
        -- Change over the last 7 days (604800 seconds)
        COALESCE(
            (
                SELECT b.quantity - t.quantity
                FROM address_balance b
                WHERE b.address = t.address 
                  AND b.unit = 'lovelace'
                  AND b.slot = (
                      SELECT MAX(b2.slot)
                      FROM address_balance b2
                      WHERE b2.address = b.address 
                        AND b2.unit = 'lovelace' 
                        AND b2.slot <= (t.current_slot - 604800) -- 7 days ago
                  )
                LIMIT 1
            ), 0) AS change_7d,

        -- Change over the last 1 month (2592000 seconds)
        COALESCE(
            (
                SELECT b.quantity - t.quantity
                FROM address_balance b
                WHERE b.address = t.address 
                  AND b.unit = 'lovelace'
                  AND b.slot = (
                      SELECT MAX(b2.slot)
                      FROM address_balance b2
                      WHERE b2.address = b.address 
                        AND b2.unit = 'lovelace' 
                        AND b2.slot <= (t.current_slot - 2592000) -- 1 month ago
                  )
                LIMIT 1
            ), 0) AS change_1m,
        
        -- Change over the last 3 months (7776000 seconds)
        COALESCE(
            (
                SELECT b.quantity - t.quantity
                FROM address_balance b
                WHERE b.address = t.address 
                  AND b.unit = 'lovelace'
                  AND b.slot = (
                      SELECT MAX(b2.slot)
                      FROM address_balance b2
                      WHERE b2.address = b.address 
                        AND b2.unit = 'lovelace' 
                        AND b2.slot <= (t.current_slot - 7776000) -- 3 months ago
                  )
                LIMIT 1
            ), 0) AS change_3m,

        -- Change over the last 6 months (15552000 seconds)
        COALESCE(
            (
                SELECT b.quantity - t.quantity
                FROM address_balance b
                WHERE b.address = t.address 
                  AND b.unit = 'lovelace'
                  AND b.slot = (
                      SELECT MAX(b2.slot)
                      FROM address_balance b2
                      WHERE b2.address = b.address 
                        AND b2.unit = 'lovelace' 
                        AND b2.slot <= (t.current_slot - 15552000) -- 6 months ago
                  )
                LIMIT 1
            ), 0) AS change_6m,

        -- Change over the last 1 year (31536000 seconds)
        COALESCE(
            (
                SELECT b.quantity - t.quantity
                FROM address_balance b
                WHERE b.address = t.address 
                  AND b.unit = 'lovelace'
                  AND b.slot = (
                      SELECT MAX(b2.slot)
                      FROM address_balance b2
                      WHERE b2.address = b.address 
                        AND b2.unit = 'lovelace' 
                        AND b2.slot <= (t.current_slot - 31536000) -- 1 year ago
                  )
                LIMIT 1
            ), 0) AS change_1y,

        -- Change over the last 3 years (94608000 seconds)
        COALESCE(
            (
                SELECT b.quantity - t.quantity
                FROM address_balance b
                WHERE b.address = t.address 
                  AND b.unit = 'lovelace'
                  AND b.slot = (
                      SELECT MAX(b2.slot)
                      FROM address_balance b2
                      WHERE b2.address = b.address 
                        AND b2.unit = 'lovelace' 
                        AND b2.slot <= (t.current_slot - 94608000) -- 3 years ago
                  )
                LIMIT 1
            ), 0) AS change_3y,

        -- Change over the last 5 years (157680000 seconds)
        COALESCE(
            (
                SELECT b.quantity - t.quantity
                FROM address_balance b
                WHERE b.address = t.address 
                  AND b.unit = 'lovelace'
                  AND b.slot = (
                      SELECT MAX(b2.slot)
                      FROM address_balance b2
                      WHERE b2.address = b.address 
                        AND b2.unit = 'lovelace' 
                        AND b2.slot <= (t.current_slot - 157680000) -- 5 years ago
                  )
                LIMIT 1
            ), 0) AS change_5y

    FROM top100_address t
)

--- 

SELECT 
    address,
    -- Convert current_balance from lovelace to ada
    current_balance / 1000000 AS current_balance_ada,
    
    -- Change over the last 1 day (in Ada)
    change_1d / 1000000 AS change_1d_ada,
    -- Percentage change over the last 1 day
    CASE WHEN change_1d != 0
        THEN (change_1d / current_balance) * 100
        ELSE 0
    END AS percent_change_1d,

    -- Change over the last 7 days (in Ada)
    change_7d / 1000000 AS change_7d_ada,
    -- Percentage change over the last 7 days
    CASE WHEN change_7d != 0
        THEN (change_7d / current_balance) * 100
        ELSE 0
    END AS percent_change_7d,

    -- Change over the last 1 month (in Ada)
    change_1m / 1000000 AS change_1m_ada,
    -- Percentage change over the last 1 month
    CASE WHEN change_1m != 0
        THEN (change_1m / current_balance) * 100
        ELSE 0
    END AS percent_change_1m,

    -- Change over the last 3 months (in Ada)
    change_3m / 1000000 AS change_3m_ada,
    -- Percentage change over the last 3 months
    CASE WHEN change_3m != 0
        THEN (change_3m / current_balance) * 100
        ELSE 0
    END AS percent_change_3m,

    -- Change over the last 6 months (in Ada)
    change_6m / 1000000 AS change_6m_ada,
    -- Percentage change over the last 6 months
    CASE WHEN change_6m != 0
        THEN (change_6m / current_balance) * 100
        ELSE 0
    END AS percent_change_6m,

    -- Change over the last 1 year (in Ada)
    change_1y / 1000000 AS change_1y_ada,
    -- Percentage change over the last 1 year
    CASE WHEN change_1y != 0
        THEN (change_1y / current_balance) * 100
        ELSE 0
    END AS percent_change_1y,

    -- Change over the last 3 years (in Ada)
    change_3y / 1000000 AS change_3y_ada,
    -- Percentage change over the last 3 years
    CASE WHEN change_3y != 0
        THEN (change_3y / current_balance) * 100
        ELSE 0
    END AS percent_change_3y,

    -- Change over the last 5 years (in Ada)
    change_5y / 1000000 AS change_5y_ada,
    -- Percentage change over the last 5 years
    CASE WHEN change_5y != 0
        THEN (change_5y / current_balance) * 100
        ELSE 0
    END AS percent_change_5y

FROM balance2.address_balance_changes
ORDER BY current_balance_ada DESC
LIMIT 100;


