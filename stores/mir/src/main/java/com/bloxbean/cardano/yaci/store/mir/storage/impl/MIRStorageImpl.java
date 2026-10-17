package com.bloxbean.cardano.yaci.store.mir.storage.impl;

import com.bloxbean.cardano.yaci.store.common.util.UUIDUtil;
import com.bloxbean.cardano.yaci.store.mir.domain.MoveInstataneousReward;
import com.bloxbean.cardano.yaci.store.mir.storage.MIRStorage;
import com.bloxbean.cardano.yaci.store.mir.storage.impl.mapper.MIRMapper;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;

import java.time.LocalDateTime;
import java.util.List;

import static com.bloxbean.cardano.yaci.store.mir.jooq.Tables.MIR;

@RequiredArgsConstructor
public class MIRStorageImpl implements MIRStorage {
    private final MIRRepository repository;
    private final MIRMapper mirMapper;
    private final DSLContext dsl;

    @Override
    public void save(List<MoveInstataneousReward> moveInstataneousRewards) {
        //Id from (tx_hash, cert_index, credential), so a block processed again overwrites its rows.
        //Credential is null for a transfer to the other pot
        var idField = UUIDUtil.idField(dsl, MIR.ID);
        LocalDateTime localDateTime = LocalDateTime.now();
        var inserts = moveInstataneousRewards.stream()
                .map(mirMapper::toMIREntity)
                .map(entity -> dsl.insertInto(MIR)
                        .set(idField, UUIDUtil.nameUUID(entity.getTxHash(), String.valueOf(entity.getCertIndex()), entity.getCredential()))
                        .set(MIR.TX_HASH, entity.getTxHash())
                        .set(MIR.CERT_INDEX, (int) entity.getCertIndex())
                        .set(MIR.POT, entity.getPot() != null ? entity.getPot().name() : null)
                        .set(MIR.CREDENTIAL, entity.getCredential())
                        .set(MIR.ADDRESS, entity.getAddress())
                        .set(MIR.AMOUNT, entity.getAmount())
                        .set(MIR.EPOCH, entity.getEpoch())
                        .set(MIR.SLOT, entity.getSlot())
                        .set(MIR.BLOCK_HASH, entity.getBlockHash())
                        .set(MIR.BLOCK, entity.getBlockNumber())
                        .set(MIR.BLOCK_TIME, entity.getBlockTime())
                        .set(MIR.UPDATE_DATETIME, localDateTime)
                        .onDuplicateKeyUpdate()
                        .set(MIR.TX_HASH, entity.getTxHash())
                        .set(MIR.CERT_INDEX, (int) entity.getCertIndex())
                        .set(MIR.POT, entity.getPot() != null ? entity.getPot().name() : null)
                        .set(MIR.CREDENTIAL, entity.getCredential())
                        .set(MIR.ADDRESS, entity.getAddress())
                        .set(MIR.AMOUNT, entity.getAmount())
                        .set(MIR.EPOCH, entity.getEpoch())
                        .set(MIR.SLOT, entity.getSlot())
                        .set(MIR.BLOCK_HASH, entity.getBlockHash())
                        .set(MIR.BLOCK, entity.getBlockNumber())
                        .set(MIR.BLOCK_TIME, entity.getBlockTime())
                        .set(MIR.UPDATE_DATETIME, localDateTime)).toList();

        dsl.batch(inserts).execute();
    }

    @Override
    public int rollbackMIRs(Long slot) {
        return repository.deleteBySlotGreaterThan(slot);
    }
}
