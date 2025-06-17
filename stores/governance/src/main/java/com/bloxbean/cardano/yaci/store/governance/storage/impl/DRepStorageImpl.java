package com.bloxbean.cardano.yaci.store.governance.storage.impl;

import com.bloxbean.cardano.yaci.core.model.certs.CertificateType;
import com.bloxbean.cardano.yaci.store.governance.domain.DRep;
import com.bloxbean.cardano.yaci.store.governance.domain.DRepStatus;
import com.bloxbean.cardano.yaci.store.governance.jooq.Tables;
import com.bloxbean.cardano.yaci.store.governance.storage.DRepStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper.DRepMapper;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.repository.DRepRepository;
import com.bloxbean.cardano.yaci.store.plugin.aspect.Plugin;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.jooq.impl.DSL.*;

@RequiredArgsConstructor
public class DRepStorageImpl implements DRepStorage {
    private static final String PLUGIN_DREP_SAVE = "governance.drep.save";
    private final DRepRepository dRepRepository;
    private final DRepMapper dRepMapper;
    private final DSLContext dslContext;

    @Override
    @Plugin(key = PLUGIN_DREP_SAVE)
    public void saveAll(List<DRep> dReps) {
        dRepRepository.saveAll(dReps.stream()
                .map(dRepMapper::toDRepEntity)
                .collect(Collectors.toList()));
    }

    @Override
    public Optional<DRep> findRecentDRepRegistration(String dRepId, Integer maxEpoch) {
        return dRepRepository.findRecentByDRepIdAndEpoch(dRepId, maxEpoch)
                .map(dRepMapper::toDRep);
    }

    @Override
    public List<DRep> findDRepsByStatusAndEpoch(DRepStatus status, Integer epoch) {
        var drepTable = dslContext.select(
                        Tables.DREP.DREP_ID,
                        Tables.DREP.DREP_HASH,
                        Tables.DREP.TX_HASH,
                        Tables.DREP.CERT_INDEX,
                        Tables.DREP.TX_INDEX,
                        Tables.DREP.CERT_TYPE,
                        Tables.DREP.STATUS,
                        Tables.DREP.EPOCH,
                        Tables.DREP.SLOT,
                        Tables.DREP.BLOCK_HASH,
                        Tables.DREP.BLOCK,
                        Tables.DREP.BLOCK_TIME,
                        Tables.DREP.UPDATE_DATETIME,
                        rowNumber().over(DSL.partitionBy(Tables.DREP.DREP_ID)
                                        .orderBy(Tables.DREP.SLOT.desc(), Tables.DREP.TX_INDEX.desc(), Tables.DREP.CERT_INDEX.desc()))
                                .as("row_num")
                )
                .from(Tables.DREP)
                .where(Tables.DREP.EPOCH.eq(epoch))
                .asTable("drep_with_rn");

        return dslContext.selectFrom(drepTable)
                .where(drepTable.field("row_num", Integer.class).eq(1))
                .and(drepTable.field(Tables.DREP.STATUS).eq(status.name()))
                .fetch()
                .map(
                        record -> DRep.builder()
                                .drepId(record.get(Tables.DREP.DREP_ID))
                                .drepHash(record.get(Tables.DREP.DREP_HASH))
                                .txHash(record.get(Tables.DREP.TX_HASH))
                                .certIndex(record.get(Tables.DREP.CERT_INDEX))
                                .txIndex(record.get(Tables.DREP.TX_INDEX))
                                .certType(record.get(Tables.DREP.CERT_TYPE) != null ? CertificateType.valueOf(record.get(Tables.DREP.CERT_TYPE)) : null)
                                .status(DRepStatus.valueOf(record.get(Tables.DREP.STATUS)))
                                .epoch(record.get(Tables.DREP.EPOCH))
                                .slot(record.get(Tables.DREP.SLOT))
                                .blockHash(record.get(Tables.DREP.BLOCK_HASH))
                                .blockNumber(record.get(Tables.DREP.BLOCK))
                                .blockTime(record.get(Tables.DREP.BLOCK_TIME))
                                .build());
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return dRepRepository.deleteBySlotGreaterThan(slot);
    }
}
