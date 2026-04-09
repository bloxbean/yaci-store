package com.bloxbean.cardano.yaci.store.blockfrost.account.dto;

import com.bloxbean.cardano.yaci.store.utxo.domain.Amount;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BFAccountAddressesTotalDto {
    private String stakeAddress;
    private List<Amount> receivedSum;
    private List<Amount> sentSum;
    private long txCount;
}
