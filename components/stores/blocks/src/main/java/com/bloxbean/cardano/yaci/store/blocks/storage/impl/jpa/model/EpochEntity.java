package com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "epoch")
public class EpochEntity {
    @Id
    @Column(name = "number")
    private long number;

    @Column(name = "block_count")
    private int blockCount;

    @Column(name = "transaction_count")
    private int transactionCount;

    @Column(name = "total_output")
    private long totalOutput;

    @Column(name = "start_time")
    private long startTime;

    @Column(name = "end_time")
    private long endTime;

    @Column(name = "max_slot")
    private long maxSlot;
}
