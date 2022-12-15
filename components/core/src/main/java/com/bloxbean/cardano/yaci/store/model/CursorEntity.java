package com.bloxbean.cardano.yaci.store.model;

import com.bloxbean.carano.yaci.store.common.model.BaseEntity;
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
@Table(name = "cursor_")
public class CursorEntity extends BaseEntity {
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "slot")
    private Long slot;

    @Column(name = "block_hash")
    private String blockHash;

    @Column(name = "block_number")
    private Long block;
}
