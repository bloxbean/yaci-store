package com.bloxbean.cardano.yaci.store.governance.storage.impl.model;

import jakarta.persistence.Column;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class LocalCommitteeMemberId {
    @Column(name = "hash")
    private String hash;

    @Column(name = "epoch")
    private Integer epoch;
}
