package com.bloxbean.cardano.yaci.store.epoch.storage.impl.jpa.model;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import lombok.*;

import java.io.Serializable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Builder
public class ProtocolParamsProposalId implements Serializable {
    @Id
    @Column(name = "tx_hash")
    private String txHash;

    @Id
    @Column(name = "key_hash")
    private String keyHash;
}
