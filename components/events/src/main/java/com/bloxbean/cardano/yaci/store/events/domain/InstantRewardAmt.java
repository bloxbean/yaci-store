package com.bloxbean.cardano.yaci.store.events.domain;

import lombok.*;

import java.math.BigInteger;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class InstantRewardAmt {
    private InstantRewardType rewardType;
    private String txHash;
    private String address;
    private BigInteger amount;
}
