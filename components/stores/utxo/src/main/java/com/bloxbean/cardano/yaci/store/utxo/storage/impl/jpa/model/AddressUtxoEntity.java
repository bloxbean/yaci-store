package com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.model;

import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.common.model.BaseEntity;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;

import java.math.BigInteger;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "address_utxo")
@IdClass(UtxoId.class)
@DynamicUpdate
public class AddressUtxoEntity extends BaseEntity {
    @Id
    @Column(name = "tx_hash")
    private String txHash;
    @Id
    @Column(name = "output_index")
    private Integer outputIndex;

    @Column(name = "slot")
    private Long slot;

    @Column(name = "block")
    private Long block;

    @Column(name = "block_hash")
    private String blockHash;

    @Column(name = "owner_addr")
    private String ownerAddr;

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

    @Lob
    @Column(name = "inline_datum")
    private String inlineDatum;

    @Lob
    @Column(name = "script_ref")
    private String scriptRef;

    @Column(name = "reference_script_hash")
    private String referenceScriptHash;

    @Column(name = "spent")
    private Boolean spent;

    @Column(name = "spent_at_slot")
    private Long spentAtSlot;

    @Column(name = "spent_tx_hash")
    private String spentTxHash;

    @Column(name = "is_collateral_return")
    private Boolean isCollateralReturn;
}
