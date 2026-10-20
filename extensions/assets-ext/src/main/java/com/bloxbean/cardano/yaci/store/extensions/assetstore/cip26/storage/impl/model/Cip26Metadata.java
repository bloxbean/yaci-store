package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.Mapping;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.validation.ValidationError;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "cip26_metadata")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Cip26Metadata {

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
     * {@link #subject}). Clients verify an entry by re-hashing this field through blake2b-224
     * and comparing to the first 28 bytes of {@code subject}.
     * <p>
     * The CIP-26 spec places <strong>no upper bound</strong> on this field — the
     * {@code cf-tokens-cip26} validator only checks it is valid hex of even length and that
     * its hash matches the subject's policy prefix. Real registry entries with time-locked
     * or k-of-n multisig scripts routinely exceed 200 hex characters, and some go much
     * further:
     * <ul>
     *   <li>MCOS (policy {@code 6f46e1304b16d884c85c62fb0eef35028facdc41aaa0fd319a152ed6})
     *       encodes a time-lock plus 3-of-4 multisig and lands at 216 hex chars.</li>
     *   <li>Incy sits at 586 hex chars — roughly 5× the old {@code VARCHAR(120)} cap.</li>
     * </ul>
     * Under the old {@code VARCHAR(120)} PostgreSQL rejected the {@code INSERT} outright
     * with {@code "value too long for type character varying(120)"};
     * {@code Cip26MetadataService#insertMapping} catches that, logs an {@code ERROR} line
     * naming the subject, and returns {@code false}. The overall sync still reports success,
     * so operators who don't tail logs or alert on {@code ERROR} counts would not notice the
     * dropped entries — but the failure is not swallowed silently at the JPA layer. The
     * column is therefore mapped to {@code TEXT} / unbounded {@code VARCHAR} — do not add a
     * {@code length} attribute here.
     *
     * @see <a href="https://github.com/cardano-foundation/CIPs/blob/main/CIP-0026/README.md">CIP-0026</a>
     */
    @Column(columnDefinition = "TEXT")
    @Nullable
    private String policy;

    /** CIP-26 name: max 50 chars per spec. A longer one is stored as written and reported in {@link #validationErrors}. */
    @Column(columnDefinition = "TEXT")
    @Nullable
    private String name;

    /** CIP-26 ticker: 2-9 chars per spec. Out-of-spec values are stored as written and reported. */
    @Column(columnDefinition = "TEXT")
    @Nullable
    private String ticker;

    /** CIP-26 url: max 250 chars per spec. Out-of-spec values are stored as written and reported. */
    @Column(columnDefinition = "TEXT")
    @Nullable
    private String url;

    /** CIP-26 description: max 500 chars per spec. Out-of-spec values are stored as written and reported. */
    @Column(columnDefinition = "TEXT")
    @Nullable
    private String description;

    /** CIP-26 decimals (well-known property 'decimals'): 0–19 in practice. A value outside
     *  {@link com.bloxbean.cardano.yaci.store.extensions.assetstore.util.TokenDecimals} is stored as written and reported. */
    @Nullable
    private Long decimals;

    /**
     * CIP-26 logo: base64-encoded image, up to ~87,400 chars per spec. Was a separate
     * {@code ft_offchain_logo} table previously; merged into this entity in the schema
     * rename. PostgreSQL TOAST keeps the column off main heap pages so non-logo queries
     * pay no scan-time cost.
     */
    @Column(columnDefinition = "TEXT")
    @Nullable
    private String logo;

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime updated;

    private String updatedBy;

    @JdbcTypeCode(SqlTypes.JSON)
    private Mapping properties;

    /**
     * How the registry entry departs from CIP-26. The entry is stored and served anyway; see ADR 0006.
     * Null on rows written before the column existed.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_errors")
    @Nullable
    private List<ValidationError> validationErrors;

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime lastSyncedAt;

}
