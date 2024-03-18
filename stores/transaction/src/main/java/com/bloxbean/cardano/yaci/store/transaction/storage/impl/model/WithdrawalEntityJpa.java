package com.bloxbean.cardano.yaci.store.transaction.storage.impl.model;

import com.bloxbean.cardano.yaci.store.common.model.JpaBlockAwareEntity;
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
@Table(name = "withdrawal")
@IdClass(WithdrawalId.class)
public class WithdrawalEntityJpa extends JpaBlockAwareEntity {
    @Id
    @Column(name = "address")
    private String address;

    @Id
    @Column(name = "tx_hash")
    private String txHash;

    @Column(name = "amount")
    private BigInteger amount;

    @Column(name = "epoch")
    private Integer epoch;

    @Column(name = "slot")
    private Long slot;

}
