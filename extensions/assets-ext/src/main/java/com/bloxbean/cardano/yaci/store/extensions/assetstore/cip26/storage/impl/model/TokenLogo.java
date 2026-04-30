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

    /**
     * Image payload associated with the token. Per the CIP-26 specification the canonical form
     * is a <strong>base64-encoded PNG</strong> (spec: {@code image/png} object, ≤ 64&nbsp;KB
     * decoded, ≤&nbsp;87400 base64 characters). Real entries in the
     * <a href="https://github.com/cardano-foundation/cardano-token-registry">cardano-token-registry</a>
     * follow this convention — raw base64, no {@code data:} URI prefix.
     * <p>
     * The {@code cf-tokens-cip26} validator enforces the 87&nbsp;400-character length cap only;
     * it does <em>not</em> validate the payload format or MIME type. In principle that means an
     * arbitrary URL (e.g. {@code "https://…/logo.png"}) or a full data URI
     * ({@code "data:image/png;base64,…"}) would also pass validation and be persisted, but such
     * values are off-spec and should not be expected from a compliant registry. Consumers that
     * want to render the logo should attempt base64 decoding first and fall back to treating
     * the value as a URL only if decoding fails.
     */
    private String logo;

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime lastSyncedAt;
}
