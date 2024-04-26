package com.bloxbean.cardano.yaci.store.account.storage.impl.model;

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
@Table(name = "address_tx_amount")
@IdClass(AddressTxAmountId.class)
@DynamicUpdate
public class AddressTxAmountEntity {
    @Id
    @Column(name = "address")
    private String address;

    @Id
    @Column(name = "unit")
    private String unit;

    @Id
    @Column(name = "tx_hash")
    private String txHash;

    @Column(name = "slot")
    private Long slot;

    @Column(name = "quantity")
    private BigInteger quantity;

    //Only set if address doesn't fit in ownerAddr field. Required for few Byron Era addr
    @Column(name = "addr_full")
    private String addrFull;

    @Column(name = "stake_address")
    private String stakeAddress;

    @Column(name = "epoch")
    private Integer epoch;

    @Column(name = "block")
    private Long blockNumber;

    @Column(name = "block_time")
    private Long blockTime;
}
