package com.bloxbean.cardano.yaci.store.governanceaggr.domain;

import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.GovActionStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class GovActionProposalInfo {

    private String txHash;

    private long index;

    private Integer expiration;

    private Integer ratifiedEpoch;

    private Integer enactedEpoch;

    private Integer droppedEpoch;

    private GovActionStatus status;
}
