package com.bloxbean.cardano.yaci.store.script.storage.impl.redis.model;

import com.redis.om.spring.annotations.Document;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Data
@Document
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class RedisDatumEntity {

    @Id
    @Column(name = "hash", nullable = false, length = 256)
    private String hash;

    @Column(name = "datum")
    private String datum;

    @Column(name = "created_at_tx", length = 256)
    private String createdAtTx;

    @CreatedDate
    @EqualsAndHashCode.Exclude
    private LocalDateTime createDateTime;

    @LastModifiedDate
    @EqualsAndHashCode.Exclude
    private LocalDateTime updateDateTime;
}
