CREATE INDEX idx_protocol_params_proposal_epoch
    ON protocol_params_proposal(epoch);

CREATE INDEX idx_protocol_params_proposal_target_epoch
    ON protocol_params_proposal(target_epoch);
