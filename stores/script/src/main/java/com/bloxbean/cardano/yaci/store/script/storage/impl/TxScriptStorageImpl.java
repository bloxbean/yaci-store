package com.bloxbean.cardano.yaci.store.script.storage.impl;

import com.bloxbean.cardano.yaci.store.common.util.UUIDUtil;
import com.bloxbean.cardano.yaci.store.plugin.aspect.Plugin;
import com.bloxbean.cardano.yaci.store.script.domain.TxScript;
import com.bloxbean.cardano.yaci.store.script.storage.TxScriptStorage;
import com.bloxbean.cardano.yaci.store.script.storage.impl.mapper.ScriptMapper;
import com.bloxbean.cardano.yaci.store.script.storage.impl.model.TxScriptEntity;
import com.bloxbean.cardano.yaci.store.script.storage.impl.repository.TxScriptRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.store.script.jooq.Tables.TRANSACTION_SCRIPTS;

@RequiredArgsConstructor
public class TxScriptStorageImpl implements TxScriptStorage {
    private final static String PLUGIN_SCRIPT_TX_SCRIPT_SAVE = "script.tx_script.save";

    private final TxScriptRepository txScriptRepository;
    private final ScriptMapper scriptMapper;
    private final DSLContext dsl;

    @Override
    @Plugin(key = PLUGIN_SCRIPT_TX_SCRIPT_SAVE)
    public void saveAll(List<TxScript> txScripts) {
        List<TxScriptEntity> txScriptEntities = txScripts.stream().map(scriptMapper::toTxScriptEntity).collect(Collectors.toList());

        //One row per redeemer, so id from (tx_hash, purpose, redeemer_index) and a block processed again overwrites its rows
        var idField = UUIDUtil.idField(dsl, TRANSACTION_SCRIPTS.ID);
        LocalDateTime localDateTime = LocalDateTime.now();
        var inserts = txScriptEntities.stream()
                .map(entity -> {
                    String purpose = entity.getPurpose() != null ? entity.getPurpose().name() : null;
                    Short scriptType = entity.getType() != null ? (short) entity.getType().ordinal() : null;
                    Long unitMem = entity.getUnitMem() != null ? entity.getUnitMem().longValue() : null;
                    Long unitSteps = entity.getUnitSteps() != null ? entity.getUnitSteps().longValue() : null;

                    return dsl.insertInto(TRANSACTION_SCRIPTS)
                            .set(idField, UUIDUtil.nameUUID(entity.getTxHash(), purpose, String.valueOf(entity.getRedeemerIndex())))
                            .set(TRANSACTION_SCRIPTS.SLOT, entity.getSlot())
                            .set(TRANSACTION_SCRIPTS.BLOCK_HASH, entity.getBlockHash())
                            .set(TRANSACTION_SCRIPTS.TX_HASH, entity.getTxHash())
                            .set(TRANSACTION_SCRIPTS.SCRIPT_HASH, entity.getScriptHash())
                            .set(TRANSACTION_SCRIPTS.SCRIPT_TYPE, scriptType)
                            .set(TRANSACTION_SCRIPTS.DATUM_HASH, entity.getDatumHash())
                            .set(TRANSACTION_SCRIPTS.REDEEMER_CBOR, entity.getRedeemerCbor())
                            .set(TRANSACTION_SCRIPTS.UNIT_MEM, unitMem)
                            .set(TRANSACTION_SCRIPTS.UNIT_STEPS, unitSteps)
                            .set(TRANSACTION_SCRIPTS.PURPOSE, purpose)
                            .set(TRANSACTION_SCRIPTS.REDEEMER_INDEX, entity.getRedeemerIndex())
                            .set(TRANSACTION_SCRIPTS.REDEEMER_DATAHASH, entity.getRedeemerDatahash())
                            .set(TRANSACTION_SCRIPTS.BLOCK, entity.getBlockNumber())
                            .set(TRANSACTION_SCRIPTS.BLOCK_TIME, entity.getBlockTime())
                            .set(TRANSACTION_SCRIPTS.UPDATE_DATETIME, localDateTime)
                            .onDuplicateKeyUpdate()
                            .set(TRANSACTION_SCRIPTS.SLOT, entity.getSlot())
                            .set(TRANSACTION_SCRIPTS.BLOCK_HASH, entity.getBlockHash())
                            .set(TRANSACTION_SCRIPTS.TX_HASH, entity.getTxHash())
                            .set(TRANSACTION_SCRIPTS.SCRIPT_HASH, entity.getScriptHash())
                            .set(TRANSACTION_SCRIPTS.SCRIPT_TYPE, scriptType)
                            .set(TRANSACTION_SCRIPTS.DATUM_HASH, entity.getDatumHash())
                            .set(TRANSACTION_SCRIPTS.REDEEMER_CBOR, entity.getRedeemerCbor())
                            .set(TRANSACTION_SCRIPTS.UNIT_MEM, unitMem)
                            .set(TRANSACTION_SCRIPTS.UNIT_STEPS, unitSteps)
                            .set(TRANSACTION_SCRIPTS.PURPOSE, purpose)
                            .set(TRANSACTION_SCRIPTS.REDEEMER_INDEX, entity.getRedeemerIndex())
                            .set(TRANSACTION_SCRIPTS.REDEEMER_DATAHASH, entity.getRedeemerDatahash())
                            .set(TRANSACTION_SCRIPTS.BLOCK, entity.getBlockNumber())
                            .set(TRANSACTION_SCRIPTS.BLOCK_TIME, entity.getBlockTime())
                            .set(TRANSACTION_SCRIPTS.UPDATE_DATETIME, localDateTime);
                }).toList();

        dsl.batch(inserts).execute();
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return txScriptRepository.deleteBySlotGreaterThan(slot);
    }

}
