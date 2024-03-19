package com.bloxbean.cardano.yaci.store.staking.storage.impl.model;

import com.bloxbean.cardano.yaci.store.common.model.BlockAwareEntity;
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
@Table(name = "pool_retirement")
@IdClass(PoolRetirementId.class)
@DynamicUpdate
public class PoolRetirementEntityJpa extends BlockAwareEntity {
    @Id
    @Column(name = "tx_hash")
    private String txHash;

    @Id
    @Column(name = "cert_index")
    private int certIndex;

    @Column(name = "pool_id")
    private String poolId;

    @Column(name = "retirement_epoch")
    private int retirementEpoch;

    @Column(name = "epoch")
    private Integer epoch; //current epoch

    @Column(name = "slot")
    private Long slot;

    @Column(name = "block_hash")
    private String blockHash;
}
