package com.bloxbean.cardano.yaci.store.governance.domain.local;

import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class LocalGovActionProposalStatus {
    private String govActionTxHash;

    private long govActionIndex;

    private GovActionStatus status;

    private Integer epoch;

    private Long slot;
}
