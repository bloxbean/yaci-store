package com.bloxbean.cardano.yaci.indexer.script.model;

import com.bloxbean.carano.yaci.indexer.common.model.BaseEntity;
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
@IdClass(TxScriptId.class)
public class TxScript extends BaseEntity {
    @Id
    @Column(name = "tx_hash")
    private String txHash;

    @Id
    @Column(name = "script_hash")
    private String scriptHash;

    @Column(name = "script_type")
    private ScriptType type;

    @Column(name = "block")
    private long block;

    @Column(name = "block_hash")
    private String blockHash;
}
