package com.bloxbean.cardano.yaci.store.staking.domain;

import com.bloxbean.cardano.yaci.core.model.Relay;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PoolDetails {
    private Integer epoch;
    private String poolId;
    private String vrfKeyHash;
    private BigInteger pledge;
    private BigInteger cost;
    private BigInteger marginNumerator;
    private BigInteger marginDenominator;
    private String rewardAccount; //stake address
    private Set<String> poolOwners;
    private List<Relay> relays;
    //pool_metadata
    private String metadataUrl;
    private String metadataHash;

    private String txHash;
    private Integer certIndex;
    private PoolStatusType status;
    private Integer retireEpoch;

    //derived
    public BigDecimal getMargin() {
        if (marginNumerator == null || marginDenominator == null)
            return BigDecimal.ZERO;

        //handle divide by zero
        if (marginDenominator == BigInteger.ZERO) {
            return BigDecimal.ZERO;
        }

        var numerator = new BigDecimal(marginNumerator);
        var denominator = new BigDecimal(marginDenominator);

        return numerator.divide(denominator);
    }
}
