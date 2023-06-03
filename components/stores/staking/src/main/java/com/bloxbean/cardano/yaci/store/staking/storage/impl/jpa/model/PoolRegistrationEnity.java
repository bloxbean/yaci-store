package com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.model;

import com.bloxbean.cardano.yaci.core.model.Relay;
import com.bloxbean.cardano.yaci.store.common.model.BaseEntity;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "pool_registration")
@IdClass(PoolRegistrationId.class)
@DynamicUpdate
public class PoolRegistrationEnity extends BaseEntity {
    @Id
    @Column(name = "tx_hash")
    private String txHash;

    @Id
    @Column(name = "cert_index")
    private int certIndex;

    @Column(name = "pool_id")
    private String poolId;

    @Column(name = "vrf_key")
    private String vrfKeyHash;

    @Column(name = "pledge")
    private BigInteger pledge;

    @Column(name = "cost")
    private BigInteger cost;

    @Column(name = "margin")
    private Double margin;

    @Column(name = "reward_account")
    private String rewardAccount;

    @Type(JsonType.class)
    private Set<String> poolOwners;

    @Type(JsonType.class)
    private List<Relay> relays;

    @Column(name = "metadata_url")
    private String metadataUrl;

    @Column
    private String metadataHash;

    @Column(name = "epoch")
    private Integer epoch;

    @Column(name = "slot")
    private Long slot;

    @Column(name = "block")
    private Long block;

    @Column(name = "block_hash")
    private String blockHash;

    @Column(name = "block_time")
    private long blockTime;
}
