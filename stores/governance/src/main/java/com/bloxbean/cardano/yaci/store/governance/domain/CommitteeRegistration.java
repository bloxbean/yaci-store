package com.bloxbean.cardano.yaci.store.governance.domain;

import com.bloxbean.cardano.yaci.core.model.certs.StakeCredType;
import com.bloxbean.cardano.yaci.store.common.domain.BlockAwareDomain;
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
public class CommitteeRegistration extends BlockAwareDomain {
    private String txHash;

    private long certIndex;

    private int txIndex;

    private Long slot;

    private String coldKey;

    private String hotKey;

    private StakeCredType credType;

    private Integer epoch;
}
