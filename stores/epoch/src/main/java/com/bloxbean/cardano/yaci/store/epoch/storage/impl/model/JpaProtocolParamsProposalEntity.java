package com.bloxbean.cardano.yaci.store.epoch.storage.impl.model;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.common.model.BlockAwareEntity;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Type;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@IdClass(ProtocolParamsProposalId.class)
@Table(name = "protocol_params_proposal")
public class JpaProtocolParamsProposalEntity extends BlockAwareEntity {
    @Id
    @Column(name = "tx_hash")
    private String txHash;

    @Id
    @Column(name = "key_hash")
    private String keyHash;

    @Type(JsonType.class)
    @Column(name = "params", columnDefinition = "json")
    private ProtocolParams params;

    @Column(name = "target_epoch")
    private Integer targetEpoch;

    @Column(name = "epoch")
    private Integer epoch;

    @Column(name = "slot")
    private Long slot;

    @Column(name = "era")
    private Era era;
}
