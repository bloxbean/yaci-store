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
     * The {@code key} field of the CIP-113 registry node datum — usually either the empty string
     * (head sentinel of the sorted linked list) or a 28-byte policy_id (56 hex chars) for a real
     * registered token. Some aiken-linked-list implementations also materialize a physical tail
     * sentinel node whose key is longer than 28 bytes, which is why this is {@code VARCHAR(128)}
     * rather than {@code VARCHAR(56)}. DO NOT shrink.
     */
    @Id
    @Column(name = "policy_id", length = 128, nullable = false)
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
     * linked list. For normal nodes this is the next policy_id (56 hex chars). For the LAST real
     * node (or a newly-initialized empty registry) this is the <strong>tail sentinel</strong>,
     * conventionally ~32 bytes of {@code 0xFF} (64+ hex chars) in the aiken-linked-list library.
     * {@code VARCHAR(128)} — do not shrink to {@code VARCHAR(56)} on the (wrong) assumption that
     * this is always a 28-byte policy_id.
     */
    @Column(name = "next_key", length = 128, nullable = false)
    private String nextKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String datum;

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime lastSyncedAt;

}
