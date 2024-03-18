package com.bloxbean.cardano.yaci.store.blocks.storage.impl.redis.model;

import com.bloxbean.cardano.yaci.store.common.model.JpaBaseEntity;
import com.redis.om.spring.annotations.Document;
import lombok.*;
import org.springframework.data.annotation.Id;

@Data
@Builder
@NoArgsConstructor
@Document
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class RedisRollbackEntity extends JpaBaseEntity {

    @Id
    private Long id;

    private String rollbackToBlockHash;

    private Long rollbackToSlot;

    private String currentBlockHash;

    private Long currentSlot;

    private Long currentBlock;
}
