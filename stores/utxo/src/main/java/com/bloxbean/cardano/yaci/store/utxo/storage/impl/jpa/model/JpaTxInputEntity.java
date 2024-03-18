package com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DynamicUpdate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "tx_input")
@IdClass(JpaUtxoId.class)
@DynamicUpdate
public class JpaTxInputEntity {
    @Id
    @Column(name = "tx_hash")
    private String txHash;
    @Id
    @Column(name = "output_index")
    private Integer outputIndex;

    @Column(name = "spent_at_slot")
    private Long spentAtSlot;

    @Column(name = "spent_at_block")
    private Long spentAtBlock;

    @Column(name = "spent_at_block_hash")
    private String spentAtBlockHash;

    @Column(name = "spent_block_time")
    private Long spentBlockTime;

    @Column(name = "spent_epoch")
    private Integer spentEpoch;

    @Column(name = "spent_tx_hash")
    private String spentTxHash;
}
