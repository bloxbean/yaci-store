-- =====================================================================================
-- yaci-store repair: mainnet block 14053493 (PostgreSQL)
--
-- yaci <= 0.3.11 (and 0.4.x <= 0.4.6) decoded the 137 transactions of mainnet block 14053493 as 0 transactions.
-- With ledger-state enabled, sync then stops at block 14053494 ("Utxos not found for all ids").
-- Without ledger-state, sync carries on and those 137 transactions are silently missing.
--
-- This script points the sync cursor at block 14053492. On the next start, yaci-store rolls
-- every enabled store back to slot 200136161 and re-fetches 14053493 with the fixed parser.
--
-- Steps:
--   1. Upgrade to the yaci-store release with the fixed yaci. Otherwise 14053493 is dropped again.
--   2. Stop the yaci-store app container. Keep postgres running.
--   3. Run this script against the store schema (docker distribution, from the install directory):
--        docker compose -f postgres-compose.yml exec -T yaci-store-postgres \
--          psql -U yaci -d yaci_store -v ON_ERROR_STOP=1 \
--          -c 'SET search_path TO yaci_store' -f - < repair-mainnet-block-14053493.sql
--      Replace user, database and schema if your deployment changed them.
--   4. Start yaci-store. The log should show a rollback to slot=200136161.
--   5. Verify:  SELECT count(*) FROM transaction WHERE block = 14053493;   -- expect 137
--
-- Safe to run on any deployment. It does nothing when:
--   * the database is not mainnet (aborts with an error unless v_force_mainnet is set)
--   * the instance has not yet synced past block 14053492
--   * block 14053493 is already stored with all 137 transactions
-- It needs no blocks store; block 14053492's values are hardcoded (checked against Koios).
-- =====================================================================================

BEGIN;

DO $$
DECLARE
    v_publisher_id  int     := 1;          -- store.event-publisher-id (default 1)
    v_force_mainnet boolean := false;      -- set true only if you have confirmed this DB is mainnet
    v_slot          bigint  := 200136161;  -- mainnet block 14053492
    v_hash          text    := '0acc389e7ce34b5ed78537d9e96e21ad6cde8422fc143e27439ff77bfef2cdc5';
    v_prev_hash     text    := '9b1c9f89164522045698a608fd52c0c8983b72c23106560f3365348df4ea9b31';
    v_is_mainnet    boolean := false;
    v_cur_slot      bigint;
    v_txs           bigint;
BEGIN
    -- 1. Mainnet check. The era table is filled by every deployment.
    --    Hashes are mainnet's first Shelley block and first Conway block.
    IF EXISTS (SELECT 1 FROM era
                WHERE block_hash IN ('aa83acbf5904c0edfe4d79b3689d3d00fcfc553cf360fd2229b98d464c28e9de',
                                     '9aa420cf998dbcceec1abaf83ab26294d278d25527e779050ab334c1fadab16c')) THEN
        v_is_mainnet := true;
    ELSIF to_regclass('block') IS NOT NULL THEN
        EXECUTE 'SELECT EXISTS (SELECT 1 FROM block WHERE slot = $1 AND hash = $2)'
            INTO v_is_mainnet USING v_slot, v_hash;
    END IF;

    IF NOT v_is_mainnet AND NOT v_force_mainnet THEN
        RAISE EXCEPTION 'Could not confirm this is a mainnet database. Nothing changed. '
                        'If it is mainnet (for example, synced from a custom start slot), set v_force_mainnet := true.';
    END IF;

    -- 2. Has the instance passed the affected block?
    SELECT max(slot) INTO v_cur_slot FROM cursor_ WHERE id = v_publisher_id;
    IF v_cur_slot IS NULL OR v_cur_slot <= v_slot THEN
        RAISE NOTICE 'Cursor (slot %) has not passed block 14053492. Nothing to do.', v_cur_slot;
        RETURN;
    END IF;

    -- 3. Already correct? (only checkable when the transaction store is enabled)
    IF to_regclass('transaction') IS NOT NULL THEN
        EXECUTE 'SELECT count(*) FROM transaction WHERE block = 14053493' INTO v_txs;
        IF v_txs = 137 THEN
            RAISE NOTICE 'Block 14053493 already has all 137 transactions. Nothing to do.';
            RETURN;
        END IF;
        RAISE NOTICE 'Block 14053493 has % of 137 transactions. Repairing.', v_txs;
    END IF;

    -- 4. Point the cursor at block 14053492.
    DELETE FROM cursor_ WHERE id = v_publisher_id AND slot > v_slot;

    INSERT INTO cursor_ (id, block_hash, slot, block_number, era, prev_block_hash,
                         create_datetime, update_datetime)
    VALUES (v_publisher_id, v_hash, v_slot, 14053492, 7, v_prev_hash,
            now() AT TIME ZONE 'UTC', now() AT TIME ZONE 'UTC')
    ON CONFLICT (id, block_hash) DO UPDATE
        SET slot = EXCLUDED.slot, block_number = EXCLUDED.block_number, era = EXCLUDED.era,
            prev_block_hash = EXCLUDED.prev_block_hash, update_datetime = EXCLUDED.update_datetime;

    RAISE NOTICE 'Cursor moved from slot % back to % (block 14053492). Start yaci-store to re-sync.',
                 v_cur_slot, v_slot;
END $$;

SELECT id, block_number, slot, block_hash FROM cursor_ ORDER BY slot DESC LIMIT 3;

COMMIT;
