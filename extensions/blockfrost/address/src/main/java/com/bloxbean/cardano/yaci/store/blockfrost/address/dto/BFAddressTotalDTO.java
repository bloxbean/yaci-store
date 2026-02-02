package com.bloxbean.cardano.yaci.store.blockfrost.address.dto;

import com.bloxbean.cardano.yaci.store.common.domain.Utxo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BFAddressTotalDTO {
    private String address;
    private List<Utxo.Amount> receivedSum;
    private List<Utxo.Amount> sentSum;
    private Long txCount;
}
