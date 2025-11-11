package com.bloxbean.cardano.yaci.store.blocks.storage.impl.model;

import com.bloxbean.cardano.yaci.store.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Entity for storing block CBOR data.
 * This is stored in a separate table to avoid impacting regular block queries.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "block_cbor")
public class BlockCborEntity extends BaseEntity {
    @Id
    @Column(name = "block_hash")
    private String blockHash;

    @Column(name = "cbor_data", nullable = false)
    private byte[] cborData;

    @Column(name = "cbor_size")
    private Integer cborSize;

    @Column(name = "slot", nullable = false)
    private Long slot;
}

