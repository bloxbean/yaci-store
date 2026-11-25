package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage.impl.model;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * One version of a CIP-113 registry node: the output that carries it (ADR 0011). A node is re-created on every insert
 * after it and every update, so a key has one row per version, and its current node is the latest by {@code slot}, then
 * {@code tx_index}. No row is updated in place.
 */
@Entity
@Table(name = "cip113_registry_node")
@IdClass(Cip113RegistryNodeId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// Business-key equals/hashCode: all three PK components are app-assigned and non-null at
// construction, so they're stable across the transient → managed → detached lifecycle.
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Cip113RegistryNode {

    /**
     * The {@code key} of the node's datum, which is also the token name of its registry NFT: the registered policy ID
     * (56 hex), or empty for the origin node, the head of the list. Up to 32 bytes (64 hex) are accepted, as the
     * parser does. {@code KEY} is reserved in H2 and MySQL; the backticks make Hibernate quote it for each dialect.
     */
    @Id
    @Column(name = "`key`", length = 64, nullable = false)
    @EqualsAndHashCode.Include
    private String key;

    @Id
    @Column(nullable = false)
    @EqualsAndHashCode.Include
    private Long slot;

    @Id
    @Column(name = "tx_hash", length = 64, nullable = false)
    @EqualsAndHashCode.Include
    private String txHash;

    /** Index of the transaction in its block: orders two versions of a node in the same slot. Not part of the key. */
    @Column(name = "tx_index", nullable = false)
    private Integer txIndex;

    @Column(name = "output_index", nullable = false)
    private Integer outputIndex;

    /** The next key in the sorted list, or the {@code 0xff} sentinel after the last node. Quoted like {@link #key}. */
    @Column(name = "`next`", length = 64, nullable = false)
    private String next;

    /** Hash of the credential (key or script) that authorises minting and burning, 56 hex; {@code null} when absent. */
    @Nullable
    @Column(name = "minting_logic_script", length = 56)
    private String mintingLogicScript;

    @Nullable
    @Column(name = "transfer_logic_script", length = 56)
    private String transferLogicScript;

    @Nullable
    @Column(name = "third_party_transfer_logic_script", length = 56)
    private String thirdPartyTransferLogicScript;

    /** {@code null} means unfracking is forbidden for the policy. */
    @Nullable
    @Column(name = "unfracking_logic_script", length = 56)
    private String unfrackingLogicScript;

    /** {@code global_state_cs} of the datum; {@code null} when empty. */
    @Nullable
    @Column(name = "global_state_policy_id", length = 56)
    private String globalStatePolicyId;

    /** The inline datum, CBOR hex, kept for re-parse and audit. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String datum;

    private LocalDateTime lastSyncedAt;

}
