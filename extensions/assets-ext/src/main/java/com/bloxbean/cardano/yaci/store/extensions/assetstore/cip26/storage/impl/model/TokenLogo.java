package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ft_offchain_logo")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TokenLogo {

    /** Matches {@link TokenMetadata#getSubject()} length (CIP-26 spec: 56-120 hex chars). */
    @Id
    @EqualsAndHashCode.Include
    @Column(length = 120)
    private String subject;

    private String logo;

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime lastSyncedAt;
}
