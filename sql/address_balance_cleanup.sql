DO
$$
    DECLARE
        deleted_rows int;
    BEGIN
        LOOP
            WITH rows_to_delete AS (SELECT address, unit, slot
                                    FROM address_balance
                                    WHERE block < :block_number
                                      AND EXISTS (SELECT 1
                                                  FROM address_balance AS a2
                                                  WHERE address_balance.address = a2.address
                                                    AND address_balance.unit = a2.unit
                                                    AND address_balance.block < a2.block)
                                    LIMIT 5000)
            DELETE
            FROM address_balance
            WHERE (address, unit, slot) IN (SELECT address, unit, slot FROM rows_to_delete);
            commit;

            GET DIAGNOSTICS deleted_rows = ROW_COUNT; -- Get the number of rows deleted
            RAISE NOTICE 'Deleted % rows in this batch.', deleted_rows; -- Output the number of rows deleted
            EXIT WHEN deleted_rows = 0; -- Exit the loop if no rows were deleted
        END LOOP;
    END
$$;
