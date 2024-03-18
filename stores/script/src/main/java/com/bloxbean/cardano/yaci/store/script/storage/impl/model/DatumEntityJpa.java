package com.bloxbean.cardano.yaci.store.script.storage.impl.model;

import com.bloxbean.cardano.yaci.store.common.model.JpaBaseEntity;
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
@Table(name = "datum")
public class DatumEntityJpa extends JpaBaseEntity {
    @Id
    @Column(name = "hash", nullable = false, length = 256)
    private String hash;

    @Column(name = "datum")
    private String datum;

    @Column(name = "created_at_tx", length = 256)
    private String createdAtTx;
}
