package com.bloxbean.cardano.yaci.indexer.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigInteger;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "transaction")
public class TxnEntity extends BaseEntity {
    @Id
    @Column(name = "tx_hash")
    private String txHash;

    @Column(name = "block")
    private long blockNumber;

    @Column(name = "slot")
    private long slot;

    @Type(type = "json")
    @Column(name = "inputs")
    private List<UtxoId> inputs;

    @Type(type = "json")
    @Column(name = "outputs")
    private List<UtxoId> outputs;

    @Column(name = "fee")
    private BigInteger fee;

    @Column(name = "ttl")
    private long ttl;

    @Column(name = "auxiliary_datahash")
    private String auxiliaryDataHash;

    @Column(name = "validity_interval_start")
    private long validityIntervalStart;

    @Column(name = "script_datahash")
    private String scriptDataHash;

    @Type(type = "json")
    @Column(name = "collateral_inputs")
    private List<UtxoId> collateralInputs;

    @Type(type = "json")
    @Column(name = "required_signers")
    private Set<String> requiredSigners;

    @Column(name = "network_id")
    private int netowrkId;

    @Type(type = "json")
    @Column(name = "collateral_return")
    private UtxoId collateralReturn;

    @Column(name = "total_collateral")
    private BigInteger totalCollateral;

    @Type(type = "json")
    @Column(name = "reference_inputs")
    private List<UtxoId> referenceInputs;

    @Column(name = "invalid")
    private boolean invalid;
}
