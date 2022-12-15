package com.bloxbean.cardano.yaci.store.script.domain;

import com.bloxbean.cardano.client.transaction.spec.ExUnits;
import com.bloxbean.cardano.client.transaction.spec.RedeemerTag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Redeemer {
    private RedeemerTag tag;
    private BigInteger getIndex;
    private String data;
    private ExUnits exUnits;
}

