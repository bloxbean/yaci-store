package com.bloxbean.cardano.yaci.store.core.storage.impl.redis.model;

import com.redis.om.spring.annotations.Document;
import com.redis.om.spring.annotations.Indexed;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Document
@AllArgsConstructor
public class RedisCursorEntity {

    @Id
    private String cursorId;
    @Indexed
    private Long eventPublisherId;
    @Indexed
    private String blockHash;
    @Indexed
    private Long slot;
    private Long block;
    private String prevBlockHash;
    private Integer era;
    private LocalDateTime createDateTime;
    private LocalDateTime updateDateTime;
}
