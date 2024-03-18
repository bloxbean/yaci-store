package com.bloxbean.cardano.yaci.store.script.storage.impl.jpa.model;

import com.bloxbean.cardano.yaci.core.model.RedeemerTag;
import com.bloxbean.cardano.yaci.store.common.model.JpaBlockAwareEntity;
import com.bloxbean.cardano.yaci.store.script.domain.ScriptType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigInteger;
import java.util.UUID;

@Data
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "transaction_scripts")
@EqualsAndHashCode(callSuper = false)
public class JpaTxScriptEntity extends JpaBlockAwareEntity {

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
