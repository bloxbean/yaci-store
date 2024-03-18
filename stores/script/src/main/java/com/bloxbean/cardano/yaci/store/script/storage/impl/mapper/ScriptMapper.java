package com.bloxbean.cardano.yaci.store.script.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.script.domain.Script;
import com.bloxbean.cardano.yaci.store.script.domain.TxScript;
import com.bloxbean.cardano.yaci.store.script.storage.impl.model.ScriptEntityJpa;
import com.bloxbean.cardano.yaci.store.script.storage.impl.model.TxScriptEntityJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class ScriptMapper {
    public abstract ScriptEntityJpa toScriptEntity(Script script);
    public abstract Script toScript(ScriptEntityJpa entity);

    public abstract TxScriptEntityJpa toTxScriptEntity(TxScript txScript);
    public abstract TxScript toTxScript(TxScriptEntityJpa entity);
}
