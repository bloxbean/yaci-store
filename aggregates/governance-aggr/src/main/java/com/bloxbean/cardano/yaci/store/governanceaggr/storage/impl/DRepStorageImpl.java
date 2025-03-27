package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl;

import com.bloxbean.cardano.yaci.core.model.certs.CertificateType;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.DRep;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.DRepStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.mapper.DRepMapper;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.DRepStatus;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.repository.DRepRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.store.governance_aggr.jooq.Tables.DREP;
import static org.jooq.impl.DSL.*;

@RequiredArgsConstructor
public class DRepStorageImpl implements DRepStorage {
    private final DRepRepository dRepRepository;
    private final DRepMapper dRepMapper;
    private final DSLContext dslContext;

    @Override
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
                        DREP.DREP_ID,
                        DREP.DREP_HASH,
                        DREP.TX_HASH,
                        DREP.CERT_INDEX,
                        DREP.TX_INDEX,
                        DREP.CERT_TYPE,
                        DREP.STATUS,
                        DREP.EPOCH,
                        DREP.SLOT,
                        DREP.BLOCK_HASH,
                        DREP.BLOCK,
                        DREP.BLOCK_TIME,
                        DREP.UPDATE_DATETIME,
                        rowNumber().over(partitionBy(DREP.DREP_ID)
                                        .orderBy(DREP.SLOT.desc(), DREP.TX_INDEX.desc(), DREP.CERT_INDEX.desc()))
                                .as("row_num")
                )
                .from(DREP)
                .where(DREP.EPOCH.eq(epoch))
                .asTable("drep_with_rn");

        return dslContext.selectFrom(drepTable)
                .where(drepTable.field("row_num", Integer.class).eq(1))
                .and(drepTable.field(DREP.STATUS).eq(status.name()))
                .fetch()
                .map(
                        record -> DRep.builder()
                                .drepId(record.get(DREP.DREP_ID))
                                .drepHash(record.get(DREP.DREP_HASH))
                                .txHash(record.get(DREP.TX_HASH))
                                .certIndex(record.get(DREP.CERT_INDEX))
                                .txIndex(record.get(DREP.TX_INDEX))
                                .certType(record.get(DREP.CERT_TYPE) != null ? CertificateType.valueOf(record.get(DREP.CERT_TYPE)) : null)
                                .status(DRepStatus.valueOf(record.get(DREP.STATUS)))
                                .epoch(record.get(DREP.EPOCH))
                                .slot(record.get(DREP.SLOT))
                                .blockHash(record.get(DREP.BLOCK_HASH))
                                .blockNumber(record.get(DREP.BLOCK))
                                .blockTime(record.get(DREP.BLOCK_TIME))
                                .build());
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return dRepRepository.deleteBySlotGreaterThan(slot);
    }
}
