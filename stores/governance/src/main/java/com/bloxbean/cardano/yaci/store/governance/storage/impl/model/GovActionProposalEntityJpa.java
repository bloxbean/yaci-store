package com.bloxbean.cardano.yaci.store.governance.storage.impl.model;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.store.common.model.BlockAwareEntity;
import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Type;

import java.math.BigInteger;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "gov_action_proposal")
@IdClass(GovActionProposalId.class)
public class GovActionProposalEntityJpa extends BlockAwareEntity {
    @Id
    @Column(name = "tx_hash")
    private String txHash;

    @Id
    @Column(name = "idx")
    private long index;

    @Column(name = "slot")
    private Long slot;

    @Column(name = "deposit")
    private BigInteger deposit;

    @Column(name = "return_address")
    private String returnAddress;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private GovActionType type;

    @Type(JsonType.class)
    @Column(columnDefinition = "json", name = "details")
    private JsonNode details;

    @Column(name = "anchor_url")
    private String anchorUrl;

    @Column(name = "anchor_hash")
    private String anchorHash;

    @Column(name = "epoch")
    private Integer epoch;
}
