package com.bloxbean.cardano.yaci.store.staking.domain;

import com.bloxbean.cardano.yaci.core.model.certs.CertificateType;
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
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class StakeRegistrationDetail {
    private String credential;
    private String address;
    private String txHash;
    private int certIndex;
    private CertificateType type;
    private int epoch;
    private long slot;

    private long block;
    private String blockHash;
    private long blockTime;
}
