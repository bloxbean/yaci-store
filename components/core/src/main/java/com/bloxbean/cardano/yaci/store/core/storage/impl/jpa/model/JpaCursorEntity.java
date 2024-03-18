package com.bloxbean.cardano.yaci.store.core.storage.impl.jpa.model;

import com.bloxbean.cardano.yaci.store.common.model.JpaBaseEntity;
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
@Table(name = "cursor_")
@IdClass(JpaCursorId.class)
public class JpaCursorEntity extends JpaBaseEntity {
    @Id
    @Column(name = "id")
    private Long id;

    @Id
    @Column(name = "block_hash")
    private String blockHash;

    @Column(name = "slot")
    private Long slot;

    @Column(name = "block_number")
    private Long block;

    @Column(name = "prev_block_hash")
    private String prevBlockHash;

    @Column(name = "era")
    private Integer era;
}
