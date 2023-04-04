package com.bloxbean.cardano.yaci.store.script.storage.impl.jpa.mapper;

import com.bloxbean.cardano.yaci.store.script.domain.Script;
import com.bloxbean.cardano.yaci.store.script.domain.TxScript;
import com.bloxbean.cardano.yaci.store.script.storage.impl.jpa.model.ScriptEntity;
import com.bloxbean.cardano.yaci.store.script.storage.impl.jpa.model.TxScriptEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class ScriptMapper {
    public abstract ScriptEntity toScriptEntity(Script script);
    public abstract Script toScript(ScriptEntity entity);

    public abstract TxScriptEntity toTxScriptEntity(TxScript txScript);
    public abstract TxScript toTxScript(TxScriptEntity entity);
}
