package com.bloxbean.carano.yaci.indexer.common.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigInteger;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Amt implements Serializable {
    private String unit;
    private String policyId;
    private String assetName;
    private BigInteger quantity;
}
