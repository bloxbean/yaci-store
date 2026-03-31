package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    public OffChainSyncState(String lastCommitHash) {
        this.lastCommitHash = lastCommitHash;
    }
}
