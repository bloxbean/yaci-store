package com.bloxbean.cardano.yaci.store.governanceaggr.domain;

import com.bloxbean.cardano.yaci.core.model.governance.Vote;
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
public class CommitteeVote {

    private String govActionTxHash;

    private int govActionIndex;

    private long slot;

    private int yesCnt;

    private int noCnt;

    private int abstainCnt;

    private String voterHash;

    private Vote vote;

    private String status;
}
