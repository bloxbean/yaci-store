package com.bloxbean.cardano.yaci.indexer.utxo.model;

import com.bloxbean.carano.yaci.indexer.common.domain.Amt;
import com.bloxbean.carano.yaci.indexer.common.model.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "address_utxo")
@IdClass(UtxoId.class)
public class AddressUtxo extends BaseEntity {
    @Id
    @Column(name = "tx_hash")
    private String txHash;
    @Id
    @Column(name = "output_index")
    private Integer outputIndex;

    @Column(name = "slot")
    private long slot;

    @Column(name = "block")
    private long block;

    @Column(name = "block_hash")
    private String blockHash;

    @Column(name = "owner_addr")
    private String ownerAddr;

    @Column(name = "owner_stake_addr")
    private String ownerStakeAddr;

    @Type(type = "json")
    private List<Amt> amounts;

    @Column(name = "data_hash")
    private String dataHash;

    @Lob
    @Column(name = "inline_datum")
    private String inlineDatum;

    @Lob
    @Column(name = "reference_script_hash")
    private String referenceScriptHash;

    @Column(name = "spent")
    private boolean spent;

    @Column(name = "spent_at_slot")
    private long spentAtSlot;

    @Column(name = "spent_tx_hash")
    private String spentTxHash;

    @Column(name = "is_collateral_return")
    private boolean isCollateralReturn;
}
