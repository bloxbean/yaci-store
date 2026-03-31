package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "off_chain_sync_state")
@Getter
@Setter
@NoArgsConstructor
public class OffChainSyncState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "last_commit_hash", length = 40, nullable = false)
    private String lastCommitHash;

    @Column(name = "last_synced_at")
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime lastSyncedAt;

    public OffChainSyncState(String lastCommitHash) {
        this.lastCommitHash = lastCommitHash;
        this.lastSyncedAt = LocalDateTime.now();
    }
}
