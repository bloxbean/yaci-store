package com.bloxbean.cardano.yaci.store.blockfrost.address.dto;

import com.bloxbean.cardano.yaci.store.common.domain.Utxo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;
import java.util.List;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BFAddressDTO {
    String address;
    List<Utxo.Amount> amount;
    String stake_address;
    String type;
    boolean script;
}
