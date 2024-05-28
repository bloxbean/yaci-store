package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "drep_info")
public class DRepInfoEntity {

    @Id
    @Column(name = "drep_hash")
    private String drepHash;

    @Column(name = "drep_id")
    private String drepId;

    @Column(name = "anchor_url")
    private String anchorUrl;

    @Column(name = "anchor_hash")
    private String anchorHash;

    @Column(name = "delegators")
    private Integer delegators;

    @Column(name = "total_stake")
    private Long totalStake;

    @Column(name = "created_at")
    private Long createdAt;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private DRepStatus status;

    @Column(name = "update_datetime")
    private LocalDateTime updateDatetime;
}
