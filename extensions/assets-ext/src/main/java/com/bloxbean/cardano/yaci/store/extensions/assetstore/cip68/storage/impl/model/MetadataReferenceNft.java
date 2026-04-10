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

    @Id
    @Column(name = "policy_id", length = 56, nullable = false)
    private String policyId;

    @Id
    @Column(name = "asset_name", length = 64, nullable = false)
    private String assetName;

    @Id
    private Long slot;

    @Column(nullable = false)
    @Builder.Default
    private Integer label = 333;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    private String ticker;

    private String url;

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
