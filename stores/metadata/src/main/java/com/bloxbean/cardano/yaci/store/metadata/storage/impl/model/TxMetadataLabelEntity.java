package com.bloxbean.cardano.yaci.store.metadata.storage.impl.model;

import com.bloxbean.cardano.yaci.store.common.model.BlockAwareEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "transaction_metadata")
public class TxMetadataLabelEntity extends BlockAwareEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "slot")
    private Long slot;

    @Column(name = "tx_hash")
    private String txHash;

    @Column(name = "label")
    private String label;

    @Column(name = "body")
    private String body;

    @Column(name = "cbor")
    private String cbor;
}
