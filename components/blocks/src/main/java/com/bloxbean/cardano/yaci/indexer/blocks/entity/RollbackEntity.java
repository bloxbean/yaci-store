package com.bloxbean.cardano.yaci.indexer.blocks.entity;

import com.bloxbean.carano.yaci.indexer.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "rollback")
public class RollbackEntity extends BaseEntity {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "rollback_to_block_hash")
    private String rollbackToBlockHash;

//    @Column(name = "rollback_to_block")
//    private long rollbackToBlock;

    @Column(name = "rollback_to_slot")
    private long rollbackToSlot;

    @Column(name = "current_block_hash")
    private String currentBlockHash;

    @Column(name = "current_slot")
    private long currentSlot;

    @Column(name = "current_block")
    private long currentBlock;
}
