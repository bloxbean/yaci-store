package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.entity;

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

    @Id
    @Column(name = "policy_id", length = 56, nullable = false)
    private String policyId;

    @Id
    private Long slot;

    @Id
    @Column(name = "tx_hash", length = 64, nullable = false)
    private String txHash;

    @Nullable
    @Column(name = "transfer_logic_script", length = 56)
    private String transferLogicScript;

    @Nullable
    @Column(name = "third_party_transfer_logic_script", length = 56)
    private String thirdPartyTransferLogicScript;

    @Nullable
    @Column(name = "global_state_policy_id", length = 56)
    private String globalStatePolicyId;

    @Nullable
    @Column(name = "next_key", length = 56)
    private String nextKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String datum;

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime lastSyncedAt;

}
