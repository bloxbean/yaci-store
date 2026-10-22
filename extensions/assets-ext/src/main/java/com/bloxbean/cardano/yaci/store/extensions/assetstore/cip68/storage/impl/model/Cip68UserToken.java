package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model;

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

/**
 * A CIP-68 fungible (333) or rich fungible (444) user token that has been seen on chain, so that the API serves the metadata of a
 * 333 or 444 subject only when its token exists (ADR 0008). The reference NFT of the same name holds the metadata, but it says
 * nothing about whether the user token was ever minted.
 * <p>
 * One row per token, written the first time the token appears in an output. It holds nothing else on purpose: the metadata is in
 * {@code cip68_metadata}.
 */
@Entity
@Table(name = "cip68_user_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Cip68UserToken {

    /** policy id + asset name of the user token, lower case hex (for example {@code <policy>0014df10<name>}). */
    @Id
    @EqualsAndHashCode.Include
    @Column(length = 120)
    private String unit;

    /** The slot of the transaction that first showed the token; a rollback to an earlier slot removes the row. */
    @Column(nullable = false)
    private Long slot;

    @Column(name = "tx_hash", nullable = false, length = 64)
    private String txHash;
}
