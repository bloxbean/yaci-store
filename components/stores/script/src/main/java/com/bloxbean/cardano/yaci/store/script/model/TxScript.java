package com.bloxbean.cardano.yaci.store.script.model;

import com.bloxbean.carano.yaci.store.common.model.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "transaction_scripts")
public class TxScript extends BaseEntity {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id")
    private Long id;

//    @Id
    @Column(name = "tx_hash")
    private String txHash;

//    @Id
    @Column(name = "script_hash")
    private String scriptHash;

    @Column(name = "slot")
    private long slot;

    @Column(name = "block")
    private long block;

    @Column(name = "block_hash")
    private String blockHash;

    @Column(name = "script_type")
    private ScriptType type;

    @Column(name = "redeemer")
    private String redeemer;

    @Column(name = "datum")
    private String datum;

    @Column(name = "datum_hash")
    private String datumHash;

}
