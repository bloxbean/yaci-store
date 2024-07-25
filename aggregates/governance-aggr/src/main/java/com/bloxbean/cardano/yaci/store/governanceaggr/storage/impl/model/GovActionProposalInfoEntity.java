package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "gov_action_proposal_info")
@IdClass(GovActionProposalInfoId.class)
public class GovActionProposalInfoEntity {

    @Id
    @Column(name = "tx_hash")
    private String txHash;

    @Id
    @Column(name = "idx")
    private long index;

    @Column(name = "expiration")
    private Integer expiration;

    @Column(name = "ratified_epoch")
    private Integer ratifiedEpoch;

    @Column(name = "enacted_epoch")
    private Integer enactedEpoch;

    @Column(name = "dropped_epoch")
    private Integer droppedEpoch;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private GovActionStatus status;

    @Column(name = "|create_datetime")
    @CreationTimestamp
    private LocalDateTime createDatetime;

    @Column(name = "update_datetime")
    @UpdateTimestamp
    private LocalDateTime updateDatetime;
}
