package com.bloxbean.cardano.yaci.store.epochaggr.storage.impl.model;

import com.bloxbean.cardano.yaci.store.common.model.JpaBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigInteger;


@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "epoch")
@EqualsAndHashCode(callSuper = true)
public class EpochEntityJpa extends JpaBaseEntity {

    @Id
    @Column(name = "number")
    private long number;

    @Column(name = "block_count")
    private int blockCount;

    @Column(name = "transaction_count")
    private long transactionCount;

    @Column(name = "total_output")
    private BigInteger totalOutput;

    @Column(name = "total_fees")
    private BigInteger totalFees;

    @Column(name = "start_time")
    private long startTime;

    @Column(name = "end_time")
    private long endTime;

    @Column(name = "max_slot")
    private long maxSlot;
}
