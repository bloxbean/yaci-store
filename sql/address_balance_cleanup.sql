-- clean_address_balance script -----------------------------------------------------------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE clean_address_balance(target_slot numeric, batch_size integer)
    LANGUAGE plpgsql
AS
$$
DECLARE
    record_count         integer;
    total_record_deleted numeric;
    start_time           timestamp;
    delete_time          timestamp;
BEGIN
    start_time = clock_timestamp();
    total_record_deleted := 0;
    LOOP
        delete_time = clock_timestamp();
        WITH rows_to_delete AS (SELECT address, unit, slot
                                FROM address_balance a1
                                WHERE a1.slot < target_slot
                                  AND EXISTS (SELECT 1
                                              FROM address_balance a2
                                              WHERE a1.address = a2.address
                                                AND a1.unit = a2.unit
                                                AND a1.slot < a2.slot)
                                LIMIT batch_size)
        DELETE
        FROM address_balance
        WHERE (address, unit, slot) IN (SELECT address, unit, slot FROM rows_to_delete);

        -- Get the number of records deleted
        GET DIAGNOSTICS record_count = ROW_COUNT;
        total_record_deleted := total_record_deleted + record_count;

        RAISE NOTICE 'Delete % records of address_balance take[%], totalTime: [%]',total_record_deleted, clock_timestamp() - delete_time, clock_timestamp() - start_time;

        -- Check if there are any records left
        IF record_count = 0 THEN
            EXIT;
        END IF;
        COMMIT;

        -- Wait for a while to reduce load on the system
        PERFORM pg_sleep(0.1); -- Wait for 0.1 second before looping again
    END LOOP;
    COMMIT;
END;
$$;