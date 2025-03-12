package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl;

import com.bloxbean.cardano.yaci.store.common.util.ListUtil;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.DRepExpiry;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.DRepExpiryStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.mapper.DRepExpiryMapper;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.repository.DRepExpiryRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static com.bloxbean.cardano.yaci.store.governance_aggr.jooq.Tables.DREP_EXPIRY;

@RequiredArgsConstructor
public class DRepExpiryStorageImpl implements DRepExpiryStorage {
    private final DRepExpiryRepository dRepExpiryRepository;
    private final DRepExpiryMapper mapper;
    private final DSLContext dsl;
    private final int BATCH_SIZE = 100;

    @Override
    public void save(List<DRepExpiry> dRepExpiryList) {
        ListUtil.partitionAndApply(dRepExpiryList.stream().toList(), BATCH_SIZE, this::saveBatch);
    }

    @Override
    public List<DRepExpiry> findByEpoch(Integer epoch) {
        return dRepExpiryRepository.findByEpoch(epoch)
                .stream().map(mapper::toDRepExpiry).toList();
    }

    private void saveBatch(Collection<DRepExpiry> collection) {
        LocalDateTime localDateTime = LocalDateTime.now();

        var inserts = collection.stream()
                .map(dRepExpiry -> dsl.insertInto(DREP_EXPIRY)
                        .set(DREP_EXPIRY.DREP_HASH, dRepExpiry.getDrepHash())
                        .set(DREP_EXPIRY.DREP_ID, dRepExpiry.getDrepId())
                        .set(DREP_EXPIRY.DORMANT_EPOCHS, dRepExpiry.getDormantEpochs())
                        .set(DREP_EXPIRY.ACTIVE_UNTIL, dRepExpiry.getActiveUntil())
                        .set(DREP_EXPIRY.EPOCH, dRepExpiry.getEpoch())
                        .set(DREP_EXPIRY.UPDATE_DATETIME, localDateTime))
                .toList();

        dsl.batch(inserts).execute();
    }
}
