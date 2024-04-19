package com.bloxbean.cardano.yaci.store.utxo.storage.impl.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.math.BigInteger;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "utxo_amount")
@IdClass(AmountId.class)
public class AmtEntity {
    @Id
    @Column(name = "tx_hash")
    private String txHash;
    @Id
    @Column(name = "output_index")
    private Integer outputIndex;
    @Id
    @Column(name = "unit")
    private String unit;

    @Column(name = "owner_addr")
    private String ownerAddr;

    @Column(name = "policy")
    private String policyId;

    @Column(name = "asset_name")
    private String assetName;

    @Column(name = "quantity")
    private BigInteger quantity;

    @Column(name = "slot")
    private Long slot;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "tx_hash", referencedColumnName = "tx_hash", insertable = false, updatable = false),
            @JoinColumn(name = "output_index", referencedColumnName = "output_index", insertable = false, updatable = false)
    })
    private AddressUtxoEntity addressUtxo;
}
