package com.bloxbean.cardano.yaci.store.blocks.storage.impl.redis.model;

import com.bloxbean.cardano.yaci.store.blocks.domain.Vrf;
import com.bloxbean.cardano.yaci.store.common.model.BaseEntity;
import com.redis.om.spring.annotations.Document;
import com.redis.om.spring.annotations.Indexed;
import com.redis.om.spring.annotations.Searchable;
import lombok.*;
import org.springframework.data.annotation.Id;

import java.math.BigInteger;

@Data
@Builder
@Document
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class BlockEntity extends BaseEntity {

    @Id
    @Indexed
    private String hash;

    @Indexed
    private Long number;

    @Indexed
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
}
