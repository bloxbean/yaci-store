package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.Mapping;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "ft_offchain_metadata")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TokenMetadata {

    /**
     * Subject = policyId (28 bytes) + optional assetName (0-32 bytes), hex-encoded.
     * CIP-26 spec: {@code minLength: 56, maxLength: 120}.
     */
    @Id
    @EqualsAndHashCode.Include
    @Column(length = 120)
    private String subject;

    /**
     * CIP-26 {@code policy} field: base16 CBOR-encoded phase-1 monetary script (a native script),
     * <strong>not</strong> the 28-byte policyId hash (that lives in the first 56 hex chars of
     * {@link #subject}). Per CIP-26: {@code minLength: 56, maxLength: 120}. Clients verify an
     * entry by re-hashing this field through blake2b-224 and comparing to the first 28 bytes of
     * {@code subject}.
     * <p>
     * The column is intentionally {@code VARCHAR(120)} — do not shrink it to {@code VARCHAR(56)}
     * on the (wrong) assumption that this is a policyId. Many real registry entries use
     * time-locked or multisig scripts whose CBOR exceeds 56 hex chars.
     *
     * @see <a href="https://github.com/cardano-foundation/CIPs/blob/main/CIP-0026/README.md">CIP-0026</a>
     */
    @Column(length = 120)
    private String policy;

    /** CIP-26 name: max 50 chars (enforced by cf-tokens-cip26 validator). */
    @Column(length = 50)
    private String name;

    /** CIP-26 ticker: 2-9 chars (enforced by cf-tokens-cip26 validator). */
    @Column(length = 9)
    private String ticker;

    /** CIP-26 url: max 250 chars (enforced by cf-tokens-cip26 validator). */
    @Column(length = 250)
    private String url;

    /** CIP-26 description: max 500 chars per spec. */
    @Column(length = 500)
    private String description;

    /** CIP-26 decimals: spec range [0, 19] inclusive (well-known property 'decimals'). */
    private Long decimals;

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime updated;

    private String updatedBy;

    @JdbcTypeCode(SqlTypes.JSON)
    private Mapping properties;

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime lastSyncedAt;
}
