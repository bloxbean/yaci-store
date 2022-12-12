package com.bloxbean.cardano.yaci.indexer.script.dto;

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
public class RedeemerDto {
    private RedeemerTag tag;
    private BigInteger getIndex;
    private String data;
    private ExUnits exUnits;
}

