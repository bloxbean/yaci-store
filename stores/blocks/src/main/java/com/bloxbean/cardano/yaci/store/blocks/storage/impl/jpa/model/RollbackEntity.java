package com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.model;

import com.bloxbean.cardano.yaci.store.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @Column(name = "rollback_to_slot")
    private Long rollbackToSlot;

    @Column(name = "current_block_hash")
    private String currentBlockHash;

    @Column(name = "current_slot")
    private Long currentSlot;

    @Column(name = "current_block")
    private Long currentBlock;
}
