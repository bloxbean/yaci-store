package com.bloxbean.cardano.yaci.store.api.governanceaggr.dto;

import com.bloxbean.cardano.client.transaction.spec.governance.DRepType;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigInteger;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class DRepDetailsDto {
    private String drepId;
    private String drepHash;
    private DRepType dRepType;
    private BigInteger deposit;
    private DRepStatus status;
    private BigInteger votingPower;
    private Long registrationSlot;
}
