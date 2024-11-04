package com.bloxbean.cardano.yaci.store.adapot.storage.impl.model;

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
@Table(name = "adapot")
@IdClass(AdaPotId.class)
public class AdaPotEntity extends BlockAwareEntity {
    @Id
    @Column(name = "epoch")
    private Integer epoch;

    @Id
    @Column(name = "slot")
    private Long slot;

    @Column(name = "deposits_stake")
    private BigInteger depositsStake;

    @Column(name = "fees")
    private BigInteger fees;

    @Column(name = "utxo")
    private BigInteger utxo;

    @Column(name = "treasury")
    private BigInteger treasury;

    @Column(name = "reserves")
    private BigInteger reserves;

    @Column(name = "rewards")
    private BigInteger rewards;

    @Column(name = "deposits_drep")
    private BigInteger depositsDrep;

    @Column(name = "deposits_proposal")
    private BigInteger depositsProposal;

}
