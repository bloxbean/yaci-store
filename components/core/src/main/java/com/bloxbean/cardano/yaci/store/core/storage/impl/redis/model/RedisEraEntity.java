package com.bloxbean.cardano.yaci.store.core.storage.impl.redis.model;

import com.redis.om.spring.annotations.Document;
import com.redis.om.spring.annotations.Indexed;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

@Data
@Document
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class RedisEraEntity {

    @Id
    @Indexed
    private int era;
    private long startSlot;
    private long block;
    private String blockHash;
}
