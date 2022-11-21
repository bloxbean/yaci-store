package com.bloxbean.cardano.yaci.indexer.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Rollback extends BaseEntity {
    @Id
    @Column(name = "block_hash")
    private String blockHash;

    @Column(name = "block")
    private long block;

    @Column(name = "epoch")
    private long epoch;
}
