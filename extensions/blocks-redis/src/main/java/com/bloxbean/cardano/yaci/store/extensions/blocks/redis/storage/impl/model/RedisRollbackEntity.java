package com.bloxbean.cardano.yaci.store.extensions.blocks.redis.storage.impl.model;

import com.redis.om.spring.annotations.Document;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Data
@Builder
@Document
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class RedisRollbackEntity {

    @Id
    private Long id;

    private String rollbackToBlockHash;

    private Long rollbackToSlot;

    private String currentBlockHash;

    private Long currentSlot;

    private Long currentBlock;

    @CreatedDate
    @EqualsAndHashCode.Exclude
    private LocalDateTime createDateTime;

    @LastModifiedDate
    @EqualsAndHashCode.Exclude
    private LocalDateTime updateDateTime;
}
