package com.bloxbean.cardano.yaci.store.common.model;

import com.redis.om.spring.annotations.Indexed;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RedisBlockAwareEntity {

    @Indexed
    private Long blockNumber;
    private Long blockTime;
    private LocalDateTime updateDateTime;
}
