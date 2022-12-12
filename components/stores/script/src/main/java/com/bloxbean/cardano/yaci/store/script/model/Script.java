package com.bloxbean.cardano.yaci.store.script.model;

import com.bloxbean.carano.yaci.store.common.model.BaseEntity;
import com.bloxbean.cardano.yaci.core.model.NativeScript;
import com.bloxbean.cardano.yaci.core.model.PlutusScript;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

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

    @Type(type = "json")
    @Column(name = "plutus_script")
    private PlutusScript plutusScript;

    @Type(type = "json")
    @Column(name = "native_script")
    private NativeScript nativeScript;
}
