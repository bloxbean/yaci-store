package com.bloxbean.cardano.yaci.store.extensions.assetstore.invalidtoken.storage.impl.model;

import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Something that should have been a token and is not usable: a CIP-26 registry file with no usable subject, a CIP-68 datum that cannot be
 * decoded, an output whose indexing failed unexpectedly, and later the failures of CIP-113 (a jail with no token in it, ...). A token that
 * is indexed, whatever is wrong with it, is not here: its errors are in the {@code validation_errors} of its own row (ADR 0006, ADR 0007).
 * <p>
 * The columns are generic on purpose: {@code standard} is the standard whose rule found the problem and {@code combined_with} the other one
 * in a combination (CIP-113 with CIP-26 or CIP-68); {@code slot}, {@code tx_hash} and {@code output_index} say where on-chain it was found,
 * {@code origin} where off-chain (a registry file); {@code details} holds what is specific to the standard. Everything but the id, the
 * standard and the reason is optional.
 */
@Entity
@Table(name = "invalid_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class InvalidToken {

    /** SHA-256 hex of what identifies the item (see {@code InvalidTokenRecorder}): the same item always has the same id. */
    @Id
    @EqualsAndHashCode.Include
    @Column(length = 64)
    private String id;

    /** The standard whose rule found the problem: {@code CIP-26}, {@code CIP-68} or {@code CIP-113}. */
    @Column(nullable = false, length = 16)
    private String standard;

    /** The other standard in a combination, for example {@code CIP-113} for a CIP-68 token in a jail. */
    @Column(name = "combined_with", length = 16)
    @Nullable
    private String combinedWith;

    /** A stable code for programs, see {@code InvalidTokenReason}. */
    @Column(name = "reason_code", nullable = false, length = 64)
    private String reasonCode;

    /** The message for people, cut to a fixed size. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "policy_id", length = 56)
    @Nullable
    private String policyId;

    /** Hex; for CIP-68 the asset name of the reference NFT. */
    @Column(name = "asset_name", length = 64)
    @Nullable
    private String assetName;

    /** The identifier as written, cut: a CIP-26 subject can be anything. */
    @Column(length = 256)
    @Nullable
    private String subject;

    /** The slot where it was found; null for what comes from Git. */
    @Nullable
    private Long slot;

    @Column(name = "tx_hash", length = 64)
    @Nullable
    private String txHash;

    /** The index of the transaction in its block, as in {@code cip68_metadata}. */
    @Column(name = "tx_index")
    @Nullable
    private Integer txIndex;

    @Column(name = "output_index")
    @Nullable
    private Integer outputIndex;

    /** Where it came from off-chain: the registry file name. */
    @Column(length = 512)
    @Nullable
    private String origin;

    /** Bounded context specific to the standard (at most 2 KB). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Nullable
    private Map<String, Object> details;

    @Column(name = "first_seen_at")
    @Nullable
    private LocalDateTime firstSeenAt;
}
