package com.bloxbean.cardano.yaci.store.transaction.storage.impl.model;

import com.bloxbean.cardano.yaci.store.common.domain.TxOuput;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.common.model.BlockAwareEntity;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Type;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "transaction")
public class TxnEntity extends BlockAwareEntity {
    @Id
    @Column(name = "tx_hash")
    private String txHash;

    @Column(name = "block_hash")
    private String blockHash;

    @Column(name = "slot")
    private Long slot;

    @Type(JsonType.class)
    @Column(name = "inputs")
    private List<UtxoKey> inputs;

    @Type(JsonType.class)
    @Column(name = "outputs")
    private List<UtxoKey> outputs;

    @Column(name = "fee")
    private BigInteger fee;

    @Column(name = "ttl")
    private Long ttl;

    @Column(name = "auxiliary_datahash")
    private String auxiliaryDataHash;

    @Column(name = "validity_interval_start")
    private Long validityIntervalStart;

    @Column(name = "script_datahash")
    private String scriptDataHash;

    @Type(JsonType.class)
    @Column(name = "collateral_inputs")
    private List<UtxoKey> collateralInputs;

    @Type(JsonType.class)
    @Column(name = "required_signers")
    private Set<String> requiredSigners;

    @Column(name = "network_id")
    private Integer netowrkId;

    @Type(JsonType.class)
    @Column(name = "collateral_return")
    private UtxoKey collateralReturn;

    @Type(JsonType.class)
    @Column(name = "collateral_return_json")
    private TxOuput collateralReturnJson;

    @Column(name = "total_collateral")
    private BigInteger totalCollateral;

    @Type(JsonType.class)
    @Column(name = "reference_inputs")
    private List<UtxoKey> referenceInputs;

    @Column(name = "invalid")
    private Boolean invalid;
}
