package com.bloxbean.cardano.yaci.store.account.domain;

import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import java.util.List;
import java.util.Set;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BalanceRollbackEvent {
    private RollbackEvent rollbackEvent;

    private List<AddressUnits> addressUnits;
    private List<String> stakeAddresses;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressUnits {
        private String address;
        private Set<String> units;
    }
}

