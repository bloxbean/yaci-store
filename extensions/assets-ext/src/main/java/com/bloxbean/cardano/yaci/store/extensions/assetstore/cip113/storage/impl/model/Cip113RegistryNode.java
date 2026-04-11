package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage.impl.model;

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
public class Cip113RegistryNode {

    /**
     * The {@code key} field of the CIP-113 registry node datum. Three possible values:
     * <ul>
     *   <li>empty string — head sentinel of the sorted linked list,</li>
     *   <li>56 hex chars — a real 28-byte policy_id,</li>
     *   <li>58–64 hex chars — tail sentinel (conventionally 32 bytes of {@code 0xFF} in the
     *       aiken-linked-list library).</li>
     * </ul>
     * {@code VARCHAR(64)} is the tightest bound that fits all three — DO NOT shrink to 56.
     */
    @Id
    @Column(name = "policy_id", length = 64, nullable = false)
    private String policyId;

    @Id
    @Column(nullable = false)
    private Long slot;

    @Id
    @Column(name = "tx_hash", length = 64, nullable = false)
    private String txHash;

    /** Aiken {@code Credential} (28-byte vkey or script hash, 56 hex chars). Protocol-bounded. */
    @Nullable
    @Column(name = "transfer_logic_script", length = 56)
    private String transferLogicScript;

    /** Aiken {@code Credential} (28-byte vkey or script hash, 56 hex chars). Protocol-bounded. */
    @Nullable
    @Column(name = "third_party_transfer_logic_script", length = 56)
    private String thirdPartyTransferLogicScript;

    /** Currency symbol of the global-state NFT (28-byte policy_id, 56 hex chars). Protocol-bounded. */
    @Nullable
    @Column(name = "global_state_policy_id", length = 56)
    private String globalStatePolicyId;

    /**
     * The {@code next} field of the CIP-113 registry node datum — the pointer in the sorted
     * linked list. Same length range as {@link #policyId}: either a real 56-hex policy_id or
     * the tail sentinel (58–64 hex chars, conventionally 32 bytes of {@code 0xFF}).
     * {@code VARCHAR(64)} is the tightest bound — DO NOT shrink to 56.
     */
    @Column(name = "next_key", length = 64, nullable = false)
    private String nextKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String datum;

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime lastSyncedAt;

}
