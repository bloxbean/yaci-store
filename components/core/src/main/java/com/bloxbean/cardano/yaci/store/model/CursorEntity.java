package com.bloxbean.cardano.yaci.store.model;

import com.bloxbean.carano.yaci.store.common.model.BaseEntity;
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
@IdClass(CursorId.class)
public class CursorEntity extends BaseEntity {
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "slot")
    private Long slot;

    @Column(name = "block_hash")
    private String blockHash;

    @Id
    @Column(name = "block_number")
    private Long block;
}
