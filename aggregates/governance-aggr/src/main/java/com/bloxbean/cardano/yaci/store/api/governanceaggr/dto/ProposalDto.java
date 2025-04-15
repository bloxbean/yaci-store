package com.bloxbean.cardano.yaci.store.api.governanceaggr.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigInteger;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
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
