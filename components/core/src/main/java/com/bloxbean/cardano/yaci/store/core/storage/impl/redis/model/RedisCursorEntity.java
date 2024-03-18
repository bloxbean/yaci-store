package com.bloxbean.cardano.yaci.store.core.storage.impl.redis.model;

import com.redis.om.spring.annotations.Document;
import com.redis.om.spring.annotations.Indexed;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Data
@Document
@NoArgsConstructor
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

    @CreatedDate
    @EqualsAndHashCode.Exclude
    private LocalDateTime createDateTime;

    @LastModifiedDate
    @EqualsAndHashCode.Exclude
    private LocalDateTime updateDateTime;
}
