package com.bloxbean.cardano.yaci.indexer.entity;

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

    @Column(name = "owner_addr")
    private String ownerAddr;

    @Column(name = "owner_stake_addr")
    private String ownerStakeAddr;

    @Column(name = "block")
    private long block;

    @Column(name = "block_hash")
    private String blockHash;

    @Type(type = "json")
    @Column(columnDefinition = "jsonb")
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
    private boolean spentAtSlot;
}
