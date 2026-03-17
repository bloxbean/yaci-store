package com.bloxbean.cardano.yaci.store.epochnonce.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class EpochNonce {
    private int epoch;
    private String nonce;
    private String evolvingNonce;
    private String candidateNonce;
    private String labNonce;
    private String lastEpochBlockNonce;
    private long slot;
    private long block;
    private long blockTime;
}
