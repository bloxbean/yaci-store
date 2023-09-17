package com.bloxbean.cardano.yaci.store.blocks.storage.impl.redis.model;

import com.bloxbean.cardano.yaci.store.common.model.BaseEntity;
import com.redis.om.spring.annotations.Document;
import lombok.*;
import org.springframework.data.annotation.Id;

@Data
@Builder
@Document
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class RollbackEntity extends BaseEntity {

    @Id
    private Long id;

    private String rollbackToBlockHash;

    private Long rollbackToSlot;

    private String currentBlockHash;

    private Long currentSlot;

    private Long currentBlock;
}
