package com.bloxbean.cardano.yaci.indexer.utxo.entity;

import lombok.*;

import javax.persistence.Column;
import java.io.Serializable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Builder
public class UtxoId implements Serializable {
    @Column(name = "tx_hash")
    private String txHash;
    @Column(name = "output_index")
    private Integer outputIndex;
}
