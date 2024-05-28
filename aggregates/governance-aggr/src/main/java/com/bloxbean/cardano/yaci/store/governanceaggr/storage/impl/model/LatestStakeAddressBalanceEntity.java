package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model;

import com.bloxbean.cardano.yaci.store.account.storage.impl.model.StakeAddressBalanceId;
import com.bloxbean.cardano.yaci.store.common.model.BlockAwareEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigInteger;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "stake_address_balance_view")
@IdClass(StakeAddressBalanceId.class)
public class LatestStakeAddressBalanceEntity extends BlockAwareEntity {
    @Id
    @Column(name = "address")
    private String address;

    @Id
    @Column(name = "slot")
    private Long slot;

    @Column(name = "quantity")
    private BigInteger quantity;

    @Column(name = "epoch")
    private Integer epoch;
}
