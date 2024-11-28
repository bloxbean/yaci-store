-- Additional postgresql specific indexes

CREATE INDEX if not exists idx_address_utxo_amounts
    ON address_utxo USING gin (amounts);
