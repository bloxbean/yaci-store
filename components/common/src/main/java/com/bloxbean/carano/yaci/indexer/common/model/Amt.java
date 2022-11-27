package com.bloxbean.carano.yaci.indexer.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Amt {
    private String unit;
    private String policyId;
    private String assetName;
    private BigInteger quantity;
}
