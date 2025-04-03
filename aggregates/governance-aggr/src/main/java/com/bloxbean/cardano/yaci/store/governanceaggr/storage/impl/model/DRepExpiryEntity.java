package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "drep_expiry")
@IdClass(DRepExpiryId.class)
public class DRepExpiryEntity {

    @Id
    @Column(name = "drep_hash")
    private String drepHash;

    @Column(name = "drep_id")
    private String drepId;

    @Column(name = "active_until")
    private Integer activeUntil;

    @Id
    @Column(name = "epoch")
    private Integer epoch;

    @UpdateTimestamp
    @Column(name = "update_datetime")
    private LocalDateTime updateDatetime;
}
