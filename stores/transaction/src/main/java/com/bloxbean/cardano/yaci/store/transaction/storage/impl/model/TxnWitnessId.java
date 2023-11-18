package com.bloxbean.cardano.yaci.store.transaction.storage.impl.model;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import lombok.*;

import java.io.Serializable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Builder
public class TxnWitnessId implements Serializable {
    @Id
    @Column(name = "tx_hash")
    private String txHash;

    @Id
    @Column(name = "idx")
    private Integer index;
}
