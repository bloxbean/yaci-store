CREATE INDEX if not exists idx_protocol_params_proposal_epoch
    ON protocol_params_proposal(epoch);

CREATE INDEX if not exists idx_protocol_params_proposal_target_epoch
    ON protocol_params_proposal(target_epoch);
