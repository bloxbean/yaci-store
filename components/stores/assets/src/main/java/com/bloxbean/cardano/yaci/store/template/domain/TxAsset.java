package com.bloxbean.cardano.yaci.store.template.domain;

import com.bloxbean.cardano.yaci.store.template.domain.MintType;
import lombok.*;

import java.math.BigInteger;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class TxAsset {
    private String txHash;
    private String policy;
    private String assetName;
    private String unit;
    private BigInteger quantity;
    private String mintType;
}
