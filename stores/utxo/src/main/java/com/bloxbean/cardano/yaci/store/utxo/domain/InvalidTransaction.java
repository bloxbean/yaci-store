package com.bloxbean.cardano.yaci.store.utxo.domain;

import com.bloxbean.cardano.yaci.helper.model.Transaction;
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
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class InvalidTransaction {
    private String txHash;
    private Long slot;
    private String blockHash;
    private Transaction transaction;
}
