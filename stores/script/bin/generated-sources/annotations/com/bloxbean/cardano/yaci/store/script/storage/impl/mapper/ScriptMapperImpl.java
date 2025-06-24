package com.bloxbean.cardano.yaci.store.script.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.script.domain.Script;
import com.bloxbean.cardano.yaci.store.script.domain.TxScript;
import com.bloxbean.cardano.yaci.store.script.storage.impl.model.ScriptEntity;
import com.bloxbean.cardano.yaci.store.script.storage.impl.model.TxScriptEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T15:09:22+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250514-1000, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class ScriptMapperImpl extends ScriptMapper {

    @Override
    public ScriptEntity toScriptEntity(Script script) {
        if ( script == null ) {
            return null;
        }

        ScriptEntity.ScriptEntityBuilder scriptEntity = ScriptEntity.builder();

        scriptEntity.content( script.getContent() );
        scriptEntity.scriptHash( script.getScriptHash() );
        scriptEntity.scriptType( script.getScriptType() );

        return scriptEntity.build();
    }

    @Override
    public Script toScript(ScriptEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Script.ScriptBuilder<?, ?> script = Script.builder();

        script.content( entity.getContent() );
        script.scriptHash( entity.getScriptHash() );
        script.scriptType( entity.getScriptType() );

        return script.build();
    }

    @Override
    public TxScriptEntity toTxScriptEntity(TxScript txScript) {
        if ( txScript == null ) {
            return null;
        }

        TxScriptEntity.TxScriptEntityBuilder<?, ?> txScriptEntity = TxScriptEntity.builder();

        txScriptEntity.blockNumber( txScript.getBlockNumber() );
        txScriptEntity.blockTime( txScript.getBlockTime() );
        txScriptEntity.blockHash( txScript.getBlockHash() );
        txScriptEntity.datumHash( txScript.getDatumHash() );
        txScriptEntity.purpose( txScript.getPurpose() );
        txScriptEntity.redeemerCbor( txScript.getRedeemerCbor() );
        txScriptEntity.redeemerDatahash( txScript.getRedeemerDatahash() );
        txScriptEntity.redeemerIndex( txScript.getRedeemerIndex() );
        txScriptEntity.scriptHash( txScript.getScriptHash() );
        txScriptEntity.slot( txScript.getSlot() );
        txScriptEntity.txHash( txScript.getTxHash() );
        txScriptEntity.type( txScript.getType() );
        txScriptEntity.unitMem( txScript.getUnitMem() );
        txScriptEntity.unitSteps( txScript.getUnitSteps() );

        return txScriptEntity.build();
    }

    @Override
    public TxScript toTxScript(TxScriptEntity entity) {
        if ( entity == null ) {
            return null;
        }

        TxScript.TxScriptBuilder<?, ?> txScript = TxScript.builder();

        txScript.blockNumber( entity.getBlockNumber() );
        txScript.blockTime( entity.getBlockTime() );
        txScript.blockHash( entity.getBlockHash() );
        txScript.datumHash( entity.getDatumHash() );
        txScript.purpose( entity.getPurpose() );
        txScript.redeemerCbor( entity.getRedeemerCbor() );
        txScript.redeemerDatahash( entity.getRedeemerDatahash() );
        txScript.redeemerIndex( entity.getRedeemerIndex() );
        txScript.scriptHash( entity.getScriptHash() );
        txScript.slot( entity.getSlot() );
        txScript.txHash( entity.getTxHash() );
        txScript.type( entity.getType() );
        txScript.unitMem( entity.getUnitMem() );
        txScript.unitSteps( entity.getUnitSteps() );

        return txScript.build();
    }
}
