CREATE INDEX idx_withdrawal_epoch_address
    ON withdrawal(epoch, address);

CREATE INDEX idx_stake_address_balance_addr_epoch_slot
    ON stake_address_balance(address, epoch, slot);

CREATE INDEX idx_delegation_addr_epoch_slot_txid_certid
    ON delegation(address, epoch, slot, tx_index, cert_index);
