package com.bloxbean.cardano.yaci.store.script.storage.impl.jpa.mapper;

import com.bloxbean.cardano.yaci.store.script.domain.Script;
import com.bloxbean.cardano.yaci.store.script.domain.TxScript;
import com.bloxbean.cardano.yaci.store.script.storage.impl.jpa.model.JpaScriptEntity;
import com.bloxbean.cardano.yaci.store.script.storage.impl.jpa.model.JpaTxScriptEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class JpaScriptMapper {
    public abstract JpaScriptEntity toScriptEntity(Script script);
    public abstract Script toScript(JpaScriptEntity entity);
    public abstract JpaTxScriptEntity toTxScriptEntity(TxScript txScript);
    public abstract TxScript toTxScript(JpaTxScriptEntity entity);
}
