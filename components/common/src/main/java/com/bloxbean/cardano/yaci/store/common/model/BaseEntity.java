package com.bloxbean.cardano.yaci.store.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.LocalDateTime;

@MappedSuperclass
public class BaseEntity {

    @Column(name = "create_datetime")
    private LocalDateTime createDateTime;

    @Column(name = "update_datetime")
    private LocalDateTime updateDateTime;

    @PrePersist
    public void prePersist() {
        this.createDateTime = LocalDateTime.now();
        this.updateDateTime = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updateDateTime = LocalDateTime.now();
    }
}
