package com.bloxbean.cardano.yaci.store.blocks.storage.impl.redis.model;

import com.bloxbean.cardano.yaci.store.common.model.BaseEntity;
import com.redis.om.spring.annotations.Document;
import lombok.*;
import org.springframework.data.annotation.Id;

import java.math.BigInteger;

@Data
@Builder
@Document
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class EpochEntity extends BaseEntity {

    @Id
    private long number;

    private int blockCount;

    private long transactionCount;

    private BigInteger totalOutput;

    private BigInteger totalFees;

    private long startTime;

    private long endTime;

    private long maxSlot;
}
