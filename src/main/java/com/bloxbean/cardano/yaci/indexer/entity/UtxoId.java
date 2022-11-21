package com.bloxbean.cardano.yaci.indexer.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import java.io.Serializable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UtxoId implements Serializable {
    @Column(name = "tx_hash")
    private String txHash;
    @Column(name = "output_index")
    private Integer outputIndex;
}
