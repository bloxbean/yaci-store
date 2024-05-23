package com.bloxbean.cardano.yaci.store.governance.storage.impl.model;

import jakarta.persistence.Column;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode
public class CommitteeMemberId implements Serializable {
    @Column(name = "hash")
    private String hash;
    @Column(name = "slot")
    private Long slot;
}
