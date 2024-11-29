package com.bloxbean.cardano.yaci.store.adapot.service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import java.math.BigInteger;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ExpectedAdaPot {
    private int epochNo;
    private BigInteger treasury;
    private BigInteger reserves;
    private BigInteger fees;
    private BigInteger deposits;
    private BigInteger utxo;
}
