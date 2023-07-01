package com.bloxbean.cardano.yaci.store.account.storage.impl.jpa.model;

import com.bloxbean.cardano.yaci.store.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigInteger;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "stake_address_balance")
@IdClass(AddressBalanceId.class)
@DynamicUpdate
public class StakeAddressBalanceEntity extends BaseEntity {
    @Id
    @Column(name = "address")
    private String address;

    @Id
    @Column(name = "unit")
    private String unit;

    @Id
    @Column(name = "slot")
    private Long slot;

    @Column(name = "quantity")
    private BigInteger quantity;

    @Column(name = "policy")
    private String policy;

    @Column(name = "asset_name")
    private String assetName;

    @Column(name = "stake_credential")
    private String stakeCredential;

    @Column(name = "block_hash")
    private String blockHash;

    @Column(name = "block")
    private Long block;

    @Column(name = "epoch")
    private Integer epoch;
}
