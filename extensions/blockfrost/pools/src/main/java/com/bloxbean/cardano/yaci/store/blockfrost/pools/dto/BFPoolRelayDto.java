package com.bloxbean.cardano.yaci.store.blockfrost.pools.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BFPoolRelayDto {
    private String ipv4;
    private String ipv6;
    private String dns;
    private String dnsSrv;
    private Integer port;
}
