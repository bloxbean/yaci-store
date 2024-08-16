package com.bloxbean.cardano.yaci.store.governance.storage.impl.model;


import jakarta.persistence.*;
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
@Table(name = "local_hard_fork_initiation")
@IdClass(LocalHardForkInitiationId.class)
public class LocalHardForkInitiationEntity {
    @Id
    @Column(name = "gov_action_tx_hash")
    private String govActionTxHash;

    @Id
    @Column(name = "gov_action_index")
    private int govActionIndex;

    @Column(name = "major_version")
    private Integer majorVersion;

    @Column(name = "minor_version")
    private Integer minorVersion;

    @Column(name = "epoch")
    private Integer epoch;

    @Column(name = "slot")
    private Long slot;

    @Column(name = "update_datetime")
    @UpdateTimestamp
    private LocalDateTime updateDatetime;
}
