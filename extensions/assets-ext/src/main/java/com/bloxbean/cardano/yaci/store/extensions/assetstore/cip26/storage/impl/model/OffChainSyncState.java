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

    /**
     * ID-based equality with a null-safe check — the id is {@code null} for transient
     * instances (before the first flush) under {@code GenerationType.IDENTITY}.
     * {@code instanceof} rather than {@code getClass()} keeps the comparison proxy-safe
     * for any future lazy-loaded associations.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OffChainSyncState that)) return false;
        return id != null && id.equals(that.id);
    }

    /**
     * Constant hashCode. Required because the id is {@code null} for transient instances
     * and becomes non-null after flush — a hashCode derived from the id would change
     * across that transition and break {@code Set}/{@code Map} semantics if the entity is
     * added to a collection before persistence. See Vlad Mihalcea's "How to implement
     * equals and hashCode using the JPA entity identifier" for the full rationale.
     */
    @Override
    public int hashCode() {
        return 31;
    }
}
