package com.bloxbean.cardano.yaci.store.assets.storage.model;

import com.bloxbean.cardano.yaci.store.assets.domain.MintType;
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
@Table(name = "assets")
public class TxAssetEntity extends BlockAwareEntity {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "slot")
    private Long slot;

    @Column(name = "tx_hash")
    private String txHash;

    @Column(name = "policy")
    private String policy;

    @Column(name = "asset_name")
    private String assetName;

    @Column(name = "unit")
    private String unit;

    @Column(name = "fingerprint")
    private String fingerprint;

    @Column(name = "quantity")
    private BigInteger quantity;

    @Column(name = "mint_type")
    @Enumerated(EnumType.STRING)
    private MintType mintType;
}
