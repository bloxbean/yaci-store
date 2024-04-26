package com.bloxbean.cardano.yaci.store.utxo.storage.impl.model;

import jakarta.persistence.Column;
import lombok.*;

import java.io.Serializable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Builder
public class AmountId implements Serializable {
    @Column(name = "tx_hash")
    private String txHash;
    @Column(name = "output_index")
    private Integer outputIndex;
    @Column(name = "unit")
    private String unit;
}
