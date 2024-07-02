package com.bloxbean.cardano.yaci.store.governance.storage.impl.model;

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
@Table(name = "constitution")
public class ConstitutionEntity {

    @Column(name = "anchor_url")
    private String anchorUrl;

    @Column(name = "anchor_hash")
    private String anchorHash;

    @Column(name = "slot")
    private Long slot;

    @Id
    @Column(name = "active_epoch")
    private Integer activeEpoch;

    @Column(name = "script")
    private String script;

    @UpdateTimestamp
    @Column(name = "update_datetime")
    private LocalDateTime updateDatetime;
}
