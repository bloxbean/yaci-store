package com.bloxbean.cardano.yaci.store.api.governance.dto;

import com.bloxbean.cardano.yaci.core.model.governance.Vote;
import com.bloxbean.cardano.yaci.core.model.governance.VoterType;
import com.bloxbean.cardano.yaci.store.common.domain.BlockAwareDomain;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VotingProcedureDto extends BlockAwareDomain {
    private UUID id;

    private String txHash;

    private long index;

    private Long slot;

    private VoterType voterType;

    private String voterHash;

    private String dRepId;

    private String govActionTxHash;

    private Integer govActionIndex;

    private Vote vote;

    private String anchorUrl;

    private String anchorHash;

    private Integer epoch;
}
