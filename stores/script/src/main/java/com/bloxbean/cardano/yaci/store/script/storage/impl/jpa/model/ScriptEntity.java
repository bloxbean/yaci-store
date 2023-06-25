package com.bloxbean.cardano.yaci.store.script.storage.impl.jpa.model;

import com.bloxbean.cardano.yaci.store.common.model.BaseEntity;
import com.bloxbean.cardano.yaci.store.script.domain.ScriptType;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "script")
public class ScriptEntity extends BaseEntity {
    @Id
    @Column(name = "script_hash")
    private String scriptHash;

    @Column(name = "script_type")
    @Enumerated(EnumType.STRING)
    private ScriptType scriptType;

    @Type(JsonType.class)
    @Column(name = "content")
    private String content;
}
