DO $$
    BEGIN
        FOR i IN 0..9 LOOP
                EXECUTE format($sql$
      CREATE TABLE address_balance_p%1$s
      PARTITION OF address_balance
      FOR VALUES WITH (MODULUS 10, REMAINDER %1$s);
    $sql$, i);
            END LOOP;
    END
$$;

DO $$
    BEGIN
        FOR i IN 0..9 LOOP
                EXECUTE format($sql$
      CREATE TABLE stake_address_balance_p%1$s
      PARTITION OF stake_address_balance
      FOR VALUES WITH (MODULUS 10, REMAINDER %1$s);
    $sql$, i);
            END LOOP;
    END
$$;

DO $$
    BEGIN
        FOR i IN 0..9 LOOP
                EXECUTE format($sql$
      CREATE TABLE address_balance_current_p%1$s
      PARTITION OF address_balance_current
      FOR VALUES WITH (MODULUS 10, REMAINDER %1$s);
    $sql$, i);
            END LOOP;
    END
$$;

DO $$
    BEGIN
        FOR i IN 0..9 LOOP
                EXECUTE format($sql$
      CREATE TABLE stake_address_balance_current_p%1$s
      PARTITION OF stake_address_balance_current
      FOR VALUES WITH (MODULUS 10, REMAINDER %1$s);
    $sql$, i);
            END LOOP;
    END
$$;
