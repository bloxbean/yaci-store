package com.bloxbean.cardano.yaci.store.core.storage.impl.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "store_state")
public class StoreStateEntity {
    @Id
    @Column(name = "state_id")
    private String stateId;

    @Column(name = "status")
    private String status;

    @Column(name = "slot")
    private Long slot;

    @Column(name = "block")
    private Long block;

    @Column(name = "block_hash")
    private String blockHash;

    @Column(name = "data")
    private String data;

    @UpdateTimestamp
    @Column(name = "update_datetime")
    private LocalDateTime updateDateTime;
}
