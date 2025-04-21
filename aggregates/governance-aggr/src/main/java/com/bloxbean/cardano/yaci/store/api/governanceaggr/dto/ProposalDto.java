package com.bloxbean.cardano.yaci.store.api.governanceaggr.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigInteger;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ProposalDto {
    private String txHash;

    private int index;

    private Long slot;

    private BigInteger deposit;

    private String returnAddress;

    private JsonNode govAction;

    private String anchorUrl;

    private String anchorHash;

    private ProposalStatus status;

    private Integer epoch;

    private Long blockNumber;

    private Long blockTime;
}
