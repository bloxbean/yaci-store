package com.bloxbean.cardano.yaci.store.transaction.domain;

import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.*;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.List;

//This class is used in the controller layer only
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TxUtxo {
    private String txHash;
    private Integer outputIndex;
    private String address;
    private String stakeAddress;
    private List<Amount> amount;
    private String dataHash;
    private String inlineDatum;
    private String scriptRef;
    private String referenceScriptHash;

    private JsonNode inlineDatumJson;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Amount implements Serializable {
        private String unit;
        private String policyId;
        private String assetName;
        @JsonSerialize(using = ToStringSerializer.class)
        private BigInteger quantity;

        public static Amount from(Amt amt) {
            return Amount.builder()
                    .unit(amt.getUnit())
                    .policyId(amt.getPolicyId())
                    .assetName(amt.getAssetName())
                    .quantity(amt.getQuantity())
                    .build();
        }

        public static List<Amount> from(List<Amt> amts) {
            return amts.stream()
                    .map(amt -> from(amt))
                    .toList();
        }
    }
}
