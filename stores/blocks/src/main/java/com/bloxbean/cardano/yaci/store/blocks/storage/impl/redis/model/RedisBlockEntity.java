package com.bloxbean.cardano.yaci.store.blocks.storage.impl.redis.model;

import com.bloxbean.cardano.yaci.store.blocks.domain.Vrf;
import com.redis.om.spring.annotations.Document;
import com.redis.om.spring.annotations.Indexed;
import com.redis.om.spring.annotations.NumericIndexed;
import com.redis.om.spring.annotations.Searchable;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;

import java.math.BigInteger;
import java.time.LocalDateTime;

@Data
@Document
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class RedisBlockEntity {

    @Id
    private String hash;

    @NumericIndexed
    private Long number;

    @NumericIndexed
    private Long slot;

    @Indexed
    private Integer epochNumber;

    private Integer epochSlot;

    private BigInteger totalOutput;

    private BigInteger totalFees;

    private Long blockTime;

    private Integer era;

    private String prevHash;

    private String issuerVkey;

    private String vrfVkey;

    private Vrf nonceVrf;

    private Vrf leaderVrf;

    private Vrf vrfResult;

    private String opCertHotVKey;

    private Integer opCertSeqNumber;

    private Integer opcertKesPeriod;

    private String opCertSigma;

    private Long blockBodySize;

    private String blockBodyHash;

    private String protocolVersion;

    private Integer noOfTxs;

    @Searchable
    private String slotLeader;

    @CreatedDate
    @EqualsAndHashCode.Exclude
    private LocalDateTime createDateTime;

    @LastModifiedDate
    @EqualsAndHashCode.Exclude
    private LocalDateTime updateDateTime;
}
