package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "metadata_reference_nft")
@IdClass(MetadataReferenceNftId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetadataReferenceNft {

    /** Policy id: exactly 28 bytes = 56 hex chars (Blake2b-224). Protocol-bounded. */
    @Id
    @Column(name = "policy_id", length = 56, nullable = false)
    private String policyId;

    /** Asset name: 0–32 bytes = 0–64 hex chars (Cardano ledger max). Protocol-bounded. */
    @Id
    @Column(name = "asset_name", length = 64, nullable = false)
    private String assetName;

    @Id
    private Long slot;

    /** CIP-68 label: 222 (NFT), 333 (FT), 444 (RFT). Only 333 is currently indexed. */
    @Column(nullable = false)
    @Builder.Default
    private Integer label = 333;

    /**
     * CIP-68 FT metadata 'name': variable UTF-8 string from the datum. 255 is lenient
     * but bounded; real tokens are typically ≤ 32 chars.
     */
    @Column(nullable = false, length = 255)
    private String name;

    /** CIP-68 FT 'description': arbitrary multi-sentence text; stored as TEXT. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /** CIP-68 FT 'ticker': short symbol; 32 is lenient (CIP-26 convention is 2–9). */
    @Column(length = 32)
    private String ticker;

    /** CIP-68 FT 'url': aligns with CIP-26 url cap of 250 chars. */
    @Column(length = 250)
    private String url;

    /** CIP-68 FT 'decimals': unsigned integer from the datum. In practice 0–19 per CIP-26 convention. */
    private Long decimals;

    @Basic(fetch = FetchType.LAZY)
    @Column(columnDefinition = "TEXT")
    private String logo;

    @Column(nullable = false)
    private Long version;

    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String datum;

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime lastSyncedAt;

}
