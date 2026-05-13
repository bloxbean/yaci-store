package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage.impl.model;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.model.Cip113CredentialType;
import jakarta.persistence.*;
import lombok.*;
import jakarta.annotation.Nullable;

import java.time.LocalDateTime;

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
// Lombok's generated equals uses `instanceof` (proxy-safe for lazy-loaded associations).
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Cip113RegistryNode {

    /**
     * The {@code key} field of the CIP-113 registry node datum.
     *
     * <h2>Dual role — read carefully</h2>
     *
     * The registry is stored on-chain as a sorted linked list (see the aiken-linked-list library).
     * That means {@code key} serves two purposes depending on which row you are looking at:
     *
     * <ul>
     *   <li><b>Real registration rows</b> — {@code key} is the 28-byte (56-hex) policy ID of the
     *       programmable token being registered. The CIP-113 spec (Aiken source: {@code registry_node.ak})
     *       describes the field as <i>"the key (currency symbol) of the programmable token policy"</i>,
     *       and in Cardano <i>currency symbol = policy ID</i>. This is the vast majority of rows
     *       and is what callers normally look up.</li>
     *   <li><b>Sentinel rows</b> — the head and tail of the sorted linked list are marker rows
     *       that store a non-policy-ID value in the {@code key} slot. They exist because a sorted
     *       linked list needs boundaries so that inserts and deletes can always reference a node
     *       "before the first real entry" or "after the last real entry". They are not registrations.</li>
     * </ul>
     *
     * <h2>Distinguishing the three cases by length</h2>
     *
     * <ul>
     *   <li><b>empty string (0 hex)</b> — head sentinel. <i>Not</i> a policy ID.</li>
     *   <li><b>exactly 56 hex chars</b> — real registered programmable token policy ID.</li>
     *   <li><b>58–64 hex chars</b> — tail sentinel. <i>Not</i> a policy ID. The aiken-linked-list
     *       implementation in use on preprod materializes a 30-byte (60-hex) sentinel of
     *       {@code 0xFF}; the upper bound of 32 bytes (64 hex) is kept for safety since the exact
     *       length is a library-level convention, not a CIP-113 protocol guarantee.</li>
     * </ul>
     *
     * {@code VARCHAR(64)} is the tightest bound that fits all three — DO NOT shrink to 56,
     * even though real policy IDs are exactly 56 hex, because that would reject sentinel rows.
     *
     * <h2>Why the column is not named {@code policy_id}</h2>
     *
     * Naming it {@code policy_id} would overclaim what is stored: two rows per registry (head
     * and tail sentinels) do not hold policy IDs. The column name {@code key} matches the
     * on-chain Aiken datum field and the parsed record field ({@code ParsedRegistryNode.key()}).
     *
     * <h2>Column quoting</h2>
     *
     * {@code KEY} is a reserved word in H2 and MySQL (but not PostgreSQL). The surrounding
     * backticks in {@code @Column(name = "`key`")} trigger Hibernate's proprietary identifier
     * quoting — at SQL generation time Hibernate applies the dialect-appropriate quote character
     * (double quotes for PostgreSQL / H2, backticks for MySQL).
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

    /** Aiken {@code Credential} (28-byte vkey or script hash, 56 hex chars). Protocol-bounded. */
    @Nullable
    @Column(name = "transfer_logic_script", length = 56)
    private String transferLogicScript;

    /**
     * Aiken {@code Credential} discriminator for {@link #transferLogicScript}. Non-null iff
     * {@code transferLogicScript} is non-null. Persisted as a string ({@code "VKEY"} or
     * {@code "SCRIPT"}) so the column is queryable from raw SQL without Java enum awareness.
     */
    @Nullable
    @Enumerated(EnumType.STRING)
    @Column(name = "transfer_logic_script_type", length = 8)
    private Cip113CredentialType transferLogicScriptType;

    /** Aiken {@code Credential} (28-byte vkey or script hash, 56 hex chars). Protocol-bounded. */
    @Nullable
    @Column(name = "third_party_transfer_logic_script", length = 56)
    private String thirdPartyTransferLogicScript;

    /** Aiken {@code Credential} discriminator for {@link #thirdPartyTransferLogicScript}. */
    @Nullable
    @Enumerated(EnumType.STRING)
    @Column(name = "third_party_transfer_logic_script_type", length = 8)
    private Cip113CredentialType thirdPartyTransferLogicScriptType;

    /** Currency symbol of the global-state NFT (28-byte policy_id, 56 hex chars). Protocol-bounded. */
    @Nullable
    @Column(name = "global_state_policy_id", length = 56)
    private String globalStatePolicyId;

    /**
     * The {@code next} field of the CIP-113 registry node datum — this row's pointer to the
     * next node in the sorted linked list. Same dual role and length range as {@link #key}:
     * it is a real 56-hex policy ID when it points at a registered entry, or a sentinel marker
     * (60 hex on preprod; up to 64 hex allowed for safety) when it points at the tail.
     * {@code VARCHAR(64)} is the tightest bound — DO NOT shrink to 56.
     *
     * <p>Backtick-quoted for the same reason as {@link #key}: {@code NEXT} is reserved in H2
     * and MySQL.
     */
    @Column(name = "`next`", length = 64, nullable = false)
    private String next;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String datum;

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime lastSyncedAt;

}
