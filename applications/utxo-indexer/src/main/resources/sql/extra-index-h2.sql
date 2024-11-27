-- Additional H2 specific indexes

CREATE INDEX if not exists idx_address_utxo_amounts
    ON address_utxo(amounts);
