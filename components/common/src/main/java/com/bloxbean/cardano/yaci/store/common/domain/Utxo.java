package com.bloxbean.cardano.yaci.store.common.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Utxo {
    private String txHash;
    private int outputIndex;
    private String address;
    private List<Amt> amount;
    private String dataHash;
    private String inlineDatum;
    private String referenceScriptHash;
}
