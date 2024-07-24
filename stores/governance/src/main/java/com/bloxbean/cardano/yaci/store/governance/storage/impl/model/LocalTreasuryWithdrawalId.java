package com.bloxbean.cardano.yaci.store.governance.storage.impl.model;

import jakarta.persistence.Column;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class LocalTreasuryWithdrawalId {
    @Column(name = "gov_action_tx_hash")
    private String govActionTxHash;

    @Column(name = "gov_action_index")
    private long govActionIndex;
}
