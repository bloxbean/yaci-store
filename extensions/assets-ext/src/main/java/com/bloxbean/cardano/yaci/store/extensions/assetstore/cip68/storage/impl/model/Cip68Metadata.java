package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "cip68_metadata")
@IdClass(Cip68MetadataId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// Business-key equals/hashCode: all three PK components are app-assigned and non-null at
// construction, so they're stable across the transient → managed → detached lifecycle.
// Lombok's generated equals uses `instanceof` (proxy-safe for lazy-loaded associations).
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Cip68Metadata {

    /** Policy id: exactly 28 bytes = 56 hex chars (Blake2b-224). Protocol-bounded. */
    @Id
    @Column(name = "policy_id", length = 56, nullable = false)
    @EqualsAndHashCode.Include
    private String policyId;

    /**
     * Asset name: 0–32 bytes = 0–64 hex chars (Cardano ledger max). Protocol-bounded.
     * Includes the CIP-68 user-token label prefix (000de140 NFT / 0014df10 FT / 001bc280 RFT)
     * so the table's primary key matches the user-facing on-chain subject.
     */
    @Id
    @Column(name = "asset_name", length = 64, nullable = false)
    @EqualsAndHashCode.Include
    private String assetName;

    @Id
    @Column(nullable = false)
    @EqualsAndHashCode.Include
    private Long slot;

    /**
     * CIP-68 label: 222 (NFT), 333 (FT), 444 (RFT). Set by Cip68Processor based on the
     * co-minted user-token prefix observed in the same transaction's outputs.
     */
    @Column(nullable = false)
    private Integer label;

    /** CIP-68 metadata 'name': variable UTF-8 string from the datum. */
    @Column(length = 255)
    private String name;

    /** CIP-68 'description': arbitrary multi-sentence text; stored as TEXT. */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** CIP-68 'ticker': short symbol (typically only label 333/444). */
    @Column(length = 32)
    private String ticker;

    /** CIP-68 'url': aligns with CIP-26 url cap of 250 chars. */
    @Column(length = 250)
    private String url;

    /** CIP-68 'decimals': unsigned integer from the datum. In practice 0–19 per CIP-26 convention.
     *  Long to match the {@code BIGINT} column and the rest of the pipeline (ParsedCip68Datum,
     *  FungibleTokenMetadata, LongProperty) — uniform type avoids boundary conversions. */
    private Long decimals;

    /** CIP-68 logo: base64-encoded image (mostly label 333 FTs). Loaded eagerly: lazy
     *  basic-field fetch requires Hibernate bytecode enhancement, which this build does
     *  not configure. Add the {@code org.hibernate.orm} gradle plugin if eager loading
     *  of this column ever becomes a hotspot. */
    @Column(columnDefinition = "TEXT")
    private String logo;

    /**
     * CIP-68 NFT 'image': IPFS URI / data: URL / https URL (mostly label 222/444).
     * Distinct from {@link #logo}, which is base64-inline. NFTs typically use {@code image}
     * (a reference) while FTs typically use {@code logo} (inlined base64).
     */
    @Column(columnDefinition = "TEXT")
    private String image;

    /** CIP-68 NFT 'mediaType': MIME type of the image (e.g. image/png). */
    @Column(name = "media_type", length = 255)
    private String mediaType;

    @Column(nullable = false)
    private Long version;

    /** Full CBOR hex of the inline datum. Variable length; kept for re-parsing/auditing.
     *  Loaded eagerly — see {@link #logo} for the rationale (no bytecode enhancement
     *  configured). This column is the largest per-row payload (typically 1–10 KB);
     *  enabling lazy fetch project-wide would be the highest-impact perf change. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String datum;

    /**
     * Catch-all JSONB. Holds the parsed datum's non-scalar bits — array of files
     * ({@code [{name, mediaType, src}, ...]}) and arbitrary collection-specific properties
     * (attributes, royalties, custom traits) that don't map to a typed column.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> properties;

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime lastSyncedAt;

}
