package com.bloxbean.cardano.yaci.store.core.storage.impl.model;

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
@Table(name = "era")
public class EraEntity {
    @Id
    @Column(name = "era")
    private int era;

    @Column(name = "start_slot")
    private long startSlot;

    @Column(name = "block")
    private long block;

    @Column(name = "block_hash")
    private String blockHash;
}
