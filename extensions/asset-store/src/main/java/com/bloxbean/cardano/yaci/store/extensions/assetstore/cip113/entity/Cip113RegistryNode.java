package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.*;
import jakarta.annotation.Nullable;

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
    @Column(name = "policy_id")
    private String policyId;

    @Id
    private Long slot;

    @Id
    @Column(name = "tx_hash")
    private String txHash;

    @Nullable
    @Column(name = "transfer_logic_script")
    private String transferLogicScript;

    @Nullable
    @Column(name = "third_party_transfer_logic_script")
    private String thirdPartyTransferLogicScript;

    @Nullable
    @Column(name = "global_state_policy_id")
    private String globalStatePolicyId;

    @Nullable
    @Column(name = "next_key")
    private String nextKey;

    private String datum;

}
