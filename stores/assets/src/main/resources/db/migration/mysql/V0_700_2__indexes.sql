CREATE INDEX idx_assets_slot
    ON assets(slot);

CREATE INDEX idx_assets_tx_hash
    ON assets(tx_hash);

CREATE INDEX idx_assets_policy
    ON assets(policy);

CREATE INDEX idx_assets_policy_assetname
    ON assets(policy, asset_name);

CREATE INDEX idx_assets_unit
    ON assets(unit);

CREATE INDEX idx_assets_fingerprint
    ON assets(fingerprint);
