package com.bloxbean.cardano.yaci.store.template.model;

import com.bloxbean.carano.yaci.store.common.model.BaseEntity;
import com.bloxbean.cardano.yaci.store.template.domain.MintType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigInteger;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "assets")
public class TxAsset extends BaseEntity {
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

    @Column(name = "quantity")
    private BigInteger quantity;

    @Column(name = "mint_type")
    private MintType mintType;
}