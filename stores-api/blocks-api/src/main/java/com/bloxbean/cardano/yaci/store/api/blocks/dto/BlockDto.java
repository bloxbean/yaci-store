package com.bloxbean.cardano.yaci.store.api.blocks.dto;

import com.bloxbean.cardano.yaci.store.blocks.domain.Vrf;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.*;

import java.math.BigInteger;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BlockDto {
    private Long time;
    private Long height;
    private Long number;
    private String hash;
    private Long slot;
    private Integer epoch;
    private Integer era;
    private Integer epochSlot;
    private String slotLeader;
    private long size;
    private int txCount;
    @JsonSerialize(using = ToStringSerializer.class)
    private BigInteger output;
    @JsonSerialize(using = ToStringSerializer.class)
    private BigInteger fees;
    private String blockVrf;
    private String opCert;
    @JsonSerialize(using = ToStringSerializer.class)
    private Integer opCertCounter;
    private Integer opCertKesPeriod;
    private String opCertSigma;
    private String previousBlock;
    private String nextBlock;
    private long confirmations;

    private String issuerVkey;
    private Vrf nonceVrf;
    private Vrf leaderVrf;
    private Vrf vrfResult;
    private String blockBodyHash;
    private String protocolVersion;
}
