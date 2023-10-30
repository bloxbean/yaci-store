package com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.model;

import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.common.model.BlockAwareEntity;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;

import java.math.BigInteger;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "address_utxo")
@IdClass(UtxoId.class)
@DynamicUpdate
public class AddressUtxoEntity extends BlockAwareEntity {
    @Id
    @Column(name = "tx_hash")
    private String txHash;
    @Id
    @Column(name = "output_index")
    private Integer outputIndex;

    @Column(name = "slot")
    private Long slot;

    @Column(name = "block_hash")
    private String blockHash;

    @Column(name = "epoch")
    private Integer epoch;

    @Column(name = "owner_addr")
    private String ownerAddr;

    //Only set if address doesn't fit in ownerAddr field. Required for few Byron Era addr
    @Column(name = "owner_addr_full")
    private String ownerAddrFull;

    @Column(name = "owner_stake_addr")
    private String ownerStakeAddr;

    @Column(name = "owner_payment_credential")
    private String ownerPaymentCredential;

    @Column(name = "owner_stake_credential")
    private String ownerStakeCredential;

    @Column(name = "lovelace_amount")
    private BigInteger lovelaceAmount;

    @Type(JsonType.class)
    private List<Amt> amounts;

    @Column(name = "data_hash")
    private String dataHash;

    @Column(name = "inline_datum")
    private String inlineDatum;

    @Column(name = "script_ref")
    private String scriptRef;

    @Column(name = "reference_script_hash")
    private String referenceScriptHash;

    @Column(name = "spent")
    private Boolean spent;

    @Column(name = "spent_at_slot")
    private Long spentAtSlot;

    @Column(name = "spent_at_block")
    private Long spentAtBlock;

    @Column(name = "spent_at_block_hash")
    private String spentAtBlockHash;

    @Column(name = "spent_block_time")
    private Long spentBlockTime;

    @Column(name = "spent_epoch")
    private Integer spentEpoch;

    @Column(name = "spent_tx_hash")
    private String spentTxHash;

    @Column(name = "is_collateral_return")
    private Boolean isCollateralReturn;
}
