package com.bloxbean.cardano.yaci.store.script.storage.impl.redis.model;

import com.bloxbean.cardano.yaci.store.script.domain.ScriptType;
import com.redis.om.spring.annotations.Document;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.*;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Data
@Document
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class RedisScriptEntity {

    @Id
    @Column(name = "script_hash")
    private String scriptHash;

    @Column(name = "script_type")
    @Enumerated(EnumType.STRING)
    private ScriptType scriptType;

    @Type(JsonType.class)
    @Column(name = "content")
    private String content;

    @CreatedDate
    @EqualsAndHashCode.Exclude
    private LocalDateTime createDateTime;

    @LastModifiedDate
    @EqualsAndHashCode.Exclude
    private LocalDateTime updateDateTime;
}
