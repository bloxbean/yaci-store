package com.bloxbean.cardano.yaci.store.account.storage.impl.model;

import com.bloxbean.cardano.yaci.store.common.model.BlockAwareEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigInteger;

@Data
@Entity
@SuperBuilder
@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@IdClass(StakeAddressBalanceId.class)
@Table(name = "stake_address_balance")
public class StakeAddressBalanceEntity extends BlockAwareEntity {

    @Id
    @Column(name = "address")
    private String address;

    @Id
    @Column(name = "slot")
    private Long slot;

    @Column(name = "quantity")
    private BigInteger quantity;

    @Column(name = "stake_credential")
    private String stakeCredential;

    @Column(name = "block_hash")
    private String blockHash;

    @Column(name = "epoch")
    private Integer epoch;
}
