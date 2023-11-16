package com.bloxbean.cardano.yaci.store.script.storage.impl.jpa.model;

import com.bloxbean.cardano.client.plutus.spec.RedeemerTag;
import com.bloxbean.cardano.yaci.store.common.model.BlockAwareEntity;
import com.bloxbean.cardano.yaci.store.script.domain.ScriptType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigInteger;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "transaction_scripts")
public class TxScriptEntity extends BlockAwareEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tx_hash")
    private String txHash;

    @Column(name = "script_hash")
    private String scriptHash;

    @Column(name = "slot")
    private Long slot;

    @Column(name = "block_hash")
    private String blockHash;

    @Column(name = "script_type")
    private ScriptType type;

    @Column(name = "redeemer_cbor")
    private String redeemerCbor;

    @Column(name = "datum_hash")
    private String datumHash;

    @Column(name = "unit_mem")
    private BigInteger unitMem;

    @Column(name = "unit_steps")
    private BigInteger unitSteps;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose")
    private RedeemerTag purpose;

    @Column(name = "redeemer_index")
    private Integer redeemerIndex;

    @Column(name = "redeemer_datahash")
    private String redeemerDatahash;

}
