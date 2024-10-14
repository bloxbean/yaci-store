package com.bloxbean.cardano.yaci.store.staking.storage.impl.model;

import com.bloxbean.cardano.yaci.store.staking.domain.PoolStatusType;
import com.bloxbean.cardano.yaci.store.common.model.BlockAwareEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigInteger;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "pool")
@IdClass(PoolId.class)
public class PoolEntity extends BlockAwareEntity {
    @Id
    @Column(name = "pool_id")
    private String poolId;

    @Id
    @Column(name = "tx_hash")
    private String txHash;

    @Id
    @Column(name = "cert_index")
    private Integer certIndex;

    @Column(name = "tx_index")
    private Integer txIndex;

    @Id
    @Column(name = "slot")
    private Long slot;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private PoolStatusType status;

    @Column(name = "amount")
    private BigInteger amount;

    @Column(name = "epoch")
    private Integer epoch;

    @Column(name = "active_epoch")
    private Integer activeEpoch;

    @Column(name = "retire_epoch")
    private Integer retireEpoch;

    @Column(name = "registration_slot")
    private Long registrationSlot;

    @Column(name = "block_hash")
    private String blockHash;
}
