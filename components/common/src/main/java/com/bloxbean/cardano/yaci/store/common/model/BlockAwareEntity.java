package com.bloxbean.cardano.yaci.store.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@MappedSuperclass
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BlockAwareEntity {

    @Column(name = "block")
    private Long blockNumber;

    @Column(name = "block_time")
    private Long blockTime;

    @Column(name = "update_datetime")
    private LocalDateTime updateDateTime;

    @PrePersist
    public void prePersist() {
        this.updateDateTime = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updateDateTime = LocalDateTime.now();
    }
}
