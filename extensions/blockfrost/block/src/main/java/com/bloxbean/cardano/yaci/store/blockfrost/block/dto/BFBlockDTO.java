package com.bloxbean.cardano.yaci.store.blockfrost.block.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BFBlockDTO {
    private Long time;
    private Long height;
    private String hash;
    private Long slot;
    private Integer epoch;
    private Integer epochSlot;
    private String slotLeader;
    private Long size;
    private Integer txCount;
    private String output;
    private String fees;
    private String blockVrf;
    private String opCert;
    private String opCertCounter;
    private String previousBlock;
    private String nextBlock;
    private Long confirmations;
}

