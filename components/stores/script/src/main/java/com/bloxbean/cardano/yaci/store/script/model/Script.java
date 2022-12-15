package com.bloxbean.cardano.yaci.store.script.model;

import com.bloxbean.carano.yaci.store.common.model.BaseEntity;
import com.bloxbean.cardano.yaci.core.model.NativeScript;
import com.bloxbean.cardano.yaci.core.model.PlutusScript;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
public class Script extends BaseEntity {
    @Id
    @Column(name = "script_hash")
    private String scriptHash;

    @Type(JsonType.class)
    @Column(name = "plutus_script")
    private PlutusScript plutusScript;

    @Type(JsonType.class)
    @Column(name = "native_script")
    private NativeScript nativeScript;
}
