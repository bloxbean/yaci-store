CREATE INDEX idx_voting_procedure_gov_action_tx_hash_gov_action_index
    ON voting_procedure (gov_action_tx_hash, gov_action_index);