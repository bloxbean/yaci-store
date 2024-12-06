package com.bloxbean.cardano.yaci.store.events.domain;

import lombok.*;

import java.math.BigInteger;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class RewardRestAmt {
    private RewardRestType type;
    private String poolId;
    private String address;
    private BigInteger amount;
}
