package com.bloxbean.cardano.yaci.store.utxo.storage.impl;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.StoreState;
import com.bloxbean.cardano.yaci.store.common.domain.TxInput;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.common.util.JsonUtil;
import com.bloxbean.cardano.yaci.store.events.internal.CommitEvent;
import com.bloxbean.cardano.yaci.store.events.model.internal.BatchEvent;
import com.bloxbean.cardano.yaci.store.plugin.aspect.Plugin;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorage;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.mapper.UtxoMapper;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.model.AddressUtxoEntity;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.model.UtxoId;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.repository.TxInputRepository;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.repository.UtxoRepository;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.JSON;
import org.jooq.Query;
import org.jooq.Table;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.*;
import static org.jooq.impl.DSL.notExists;
import static org.jooq.impl.DSL.row;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.selectOne;

/**
 * Writes address_utxo and tx_input, and, while {@link UnspentUtxoTable#isActive() address_utxo_unspent
 * is active}, keeps it equal to address_utxo minus tx_input.
 *
 * <p>address_utxo_unspent is written from the same calls as address_utxo, so plugin filters, Byron
 * and genesis outputs apply to both tables alike. The batch is settled at {@link #handleCommit}, when
 * every block of it has been processed (in parallel mode a block's spends can be saved before the output
 * they spend, which another block of the same batch writes, and {@code UtxoProcessor} saves a block's
 * spends before its outputs):
 * <ul>
 *   <li>{@link #saveUnspent} writes address_utxo and remembers the outputs, and {@link #saveSpent}
 *   writes tx_input and remembers the spent keys;</li>
 *   <li>at commit, the outputs the batch did not spend are upserted into address_utxo_unspent, and
 *   the spent outputs created by earlier batches are deleted. An output created and spent within the
 *   batch, a large share during the initial sync, is never written. Nothing is read back: the outputs
 *   are the objects {@link UtxoCache} holds until the commit anyway. The same transaction records the
 *   batch's last block as the block the table is kept to;</li>
 *   <li>a rollback deletes the rows created after the rollback point and restores the outputs
 *   whose spend it removes, before tx_input is trimmed, and moves that record back to the point.</li>
 * </ul>
 */
@Slf4j
public class UtxoStorageImpl implements UtxoStorage {
    private static final String PLUGIN_UNSPENT = "utxo.unspent.save";
    private static final String PLUGIN_SPENT = "utxo.spent.save";
    private static final int UNSPENT_WRITE_BATCH_SIZE = 1000;

    private final UtxoRepository utxoRepository;
    private final TxInputRepository spentOutputRepository;
    private final DSLContext dsl;
    private final UtxoMapper mapper = UtxoMapper.INSTANCE;
    private final UtxoCache utxoCache;
    private final PlatformTransactionManager transactionManager;
    private final UnspentUtxoTable unspentUtxoTable;
    private final TxInputAddrHash txInputAddrHash;

    //Slots of the spends saved since the last commit, while tx_input.addr_hash is written (see TxInputAddrHash)
    private final AtomicLong addrHashFromSlot = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong addrHashToSlot = new AtomicLong(Long.MIN_VALUE);

    //Outputs saved and keys spent since the last commit, settled in address_utxo_unspent at commit
    private final Map<UtxoKey, PendingOutput> pendingOutputs = new ConcurrentHashMap<>();
    private final Set<UtxoKey> pendingSpentKeys = ConcurrentHashMap.newKeySet();

    private record PendingOutput(AddressUtxo addressUtxo, LocalDateTime savedAt) {
    }

    private TransactionTemplate transactionTemplate;
    private TransactionTemplate commitTransactionTemplate;

    @Value("${store.utxo.pruning-batch-size:3000}")
    private int pruningBatchSize = 3000;

    public UtxoStorageImpl(UtxoRepository utxoRepository, TxInputRepository spentOutputRepository, DSLContext dsl,
                           UtxoCache utxoCache, PlatformTransactionManager transactionManager,
                           UnspentUtxoTable unspentUtxoTable) {
        this(utxoRepository, spentOutputRepository, dsl, utxoCache, transactionManager, unspentUtxoTable, null);
    }

    public UtxoStorageImpl(UtxoRepository utxoRepository, TxInputRepository spentOutputRepository, DSLContext dsl,
                           UtxoCache utxoCache, PlatformTransactionManager transactionManager,
                           UnspentUtxoTable unspentUtxoTable, TxInputAddrHash txInputAddrHash) {
        this.utxoRepository = utxoRepository;
        this.spentOutputRepository = spentOutputRepository;
        this.dsl = dsl;
        this.utxoCache = utxoCache;
        this.transactionManager = transactionManager;
        this.unspentUtxoTable = unspentUtxoTable;
        this.txInputAddrHash = txInputAddrHash;

        init();
    }

    public void init() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        //Joins the block's transaction in sync mode, where the commit is part of it
        commitTransactionTemplate = new TransactionTemplate(transactionManager);
        commitTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
    }

    private boolean isUnspentTableEnabled() {
        return unspentUtxoTable != null && unspentUtxoTable.isActive();
    }

    @Override
    public Optional<AddressUtxo> findById(String txHash, int outputIndex) {
        var cacheUtxo = utxoCache.get(txHash, outputIndex);
        if (cacheUtxo.isPresent())
            return cacheUtxo;
        else {
            var savedUtxo = utxoRepository.findById(new UtxoId(txHash, outputIndex))
                    .map(mapper::toAddressUtxo);
            savedUtxo.ifPresent(utxoCache::add);

            return savedUtxo;
        }
    }

    @Override
    public List<AddressUtxo> findAllByIds(List<UtxoKey> utxoKeys) {

        var cacheResult = utxoCache.get(utxoKeys);
        if (cacheResult._2 == null)
            return cacheResult._1;

        List<UtxoKey> notFoundKeys = cacheResult._2;

        List<UtxoId> utxoIds = notFoundKeys.stream()
                .map(utxoKey -> new UtxoId(utxoKey.getTxHash(), utxoKey.getOutputIndex()))
                .toList();

        var savedUtxos = utxoRepository.findAllById(utxoIds)
                .stream().map(mapper::toAddressUtxo)
                .toList();

        //Add remaining utxos to cache
        if (savedUtxos != null)
            savedUtxos.forEach(utxoCache::add);

        List<AddressUtxo> finalUtxos = new ArrayList<>();
        finalUtxos.addAll(cacheResult._1);
        finalUtxos.addAll(savedUtxos);

        return finalUtxos;
    }

    @Override
    @Transactional
    public int deleteUnspentBySlotGreaterThan(Long slot) {
        if (isUnspentTableEnabled()) {
            int deleted = dsl.deleteFrom(ADDRESS_UTXO_UNSPENT)
                    .where(ADDRESS_UTXO_UNSPENT.SLOT.gt(slot))
                    .execute();
            log.info("Rollback -- {} address_utxo_unspent records", deleted);
        }

        return utxoRepository.deleteBySlotGreaterThan(slot);
    }

    @Override
    @Transactional
    public int deleteSpentBySlotGreaterThan(Long slot) {
        if (isUnspentTableEnabled()) {
            //Must run before tx_input is trimmed: the outputs to restore are the ones whose spend is removed
            int restored = restoreUnspentSpentAfter(slot);
            log.info("Rollback -- {} address_utxo_unspent records restored", restored);

            //Outputs and spends not yet committed belong to the blocks being rolled back
            pendingOutputs.clear();
            pendingSpentKeys.clear();
            unspentUtxoTable.rolledBackTo(slot, true);
        } else if (unspentUtxoTable != null) {
            unspentUtxoTable.rolledBackTo(slot, false);
        }

        if (txInputAddrHash != null) {
            txInputAddrHash.rolledBackTo(slot);
            addrHashFromSlot.set(Long.MAX_VALUE);
            addrHashToSlot.set(Long.MIN_VALUE);
        }

        return spentOutputRepository.deleteBySpentAtSlotGreaterThan(slot);
    }

    private int restoreUnspentSpentAfter(Long slot) {
        return dsl.insertInto(ADDRESS_UTXO_UNSPENT, ADDRESS_UTXO_UNSPENT.fields())
                .select(select(UnspentUtxoTable.addressUtxoColumns())
                        .from(TX_INPUT)
                        .join(ADDRESS_UTXO)
                        .on(ADDRESS_UTXO.TX_HASH.eq(TX_INPUT.TX_HASH))
                        .and(ADDRESS_UTXO.OUTPUT_INDEX.eq(TX_INPUT.OUTPUT_INDEX))
                        .where(TX_INPUT.SPENT_AT_SLOT.gt(slot))
                        .and(ADDRESS_UTXO.SLOT.le(slot))
                        .and(notExists(selectOne().from(ADDRESS_UTXO_UNSPENT)
                                .where(ADDRESS_UTXO_UNSPENT.TX_HASH.eq(ADDRESS_UTXO.TX_HASH))
                                .and(ADDRESS_UTXO_UNSPENT.OUTPUT_INDEX.eq(ADDRESS_UTXO.OUTPUT_INDEX)))))
                .execute();
    }

    @Override
    @Transactional
    public int deleteBySpentAndBlockLessThan(Long block) {
        int count = 0;
        int totalCount = 0;

        int limit = pruningBatchSize;

        do {
            count = transactionTemplate.execute(status ->  {
                var spentSubQuery = dsl.select(TX_INPUT.TX_HASH, TX_INPUT.OUTPUT_INDEX)
                        .from(TX_INPUT)
                        .where(TX_INPUT.SPENT_AT_BLOCK.lt(block))
                        .orderBy(TX_INPUT.SPENT_AT_BLOCK.asc())
                        .limit(limit);

                int addressUtxoCount = dsl.deleteFrom(ADDRESS_UTXO)
                        .where(row(ADDRESS_UTXO.TX_HASH, ADDRESS_UTXO.OUTPUT_INDEX)
                                .in(spentSubQuery))
                        .execute();

                var txInputQuery = dsl.deleteFrom(TX_INPUT)
                        .where(TX_INPUT.SPENT_AT_BLOCK.lt(block))
                        .orderBy(TX_INPUT.SPENT_AT_BLOCK.asc())
                        .limit(limit);

                int txInputCount = txInputQuery.execute();

                if (log.isDebugEnabled())
                    log.debug("Deleted {} address_utxo and {} utxo_amount records and tx_inputs: {}",
                            addressUtxoCount, txInputCount);

                int delCount  = addressUtxoCount + txInputCount;
                return delCount;
            });

            totalCount += count;
        } while(count > 0);

        return totalCount;
    }

    @Override
    @Transactional
    @Plugin(key = PLUGIN_UNSPENT)
    public void saveUnspent(List<AddressUtxo> addressUtxoList) {
        List<AddressUtxoEntity> addressUtxoEntities = addressUtxoList.stream()
                .map(mapper::toAddressUtxoEntity)
                .toList();

        LocalDateTime localDateTime = LocalDateTime.now();
        var inserts = addressUtxoEntities.stream()
                .map(addressUtxo -> upsert(ADDRESS_UTXO, addressUtxo, localDateTime))
                .toList();

        dsl.batch(inserts).execute();

        if (isUnspentTableEnabled()) {
            //A later save of the same output (pointer-address resolution at commit) replaces it
            addressUtxoList.forEach(addressUtxo -> pendingOutputs.put(
                    new UtxoKey(addressUtxo.getTxHash(), addressUtxo.getOutputIndex()), new PendingOutput(addressUtxo, localDateTime)));
        }

        addressUtxoList.forEach(utxoCache::add);
    }

    /**
     * Insert-or-update of one output into address_utxo or address_utxo_unspent, which have the same columns.
     * An update overwrites every column, as the pointer-address resolution at commit re-saves outputs.
     */
    private Query upsert(Table<?> table, AddressUtxoEntity addressUtxo, LocalDateTime localDateTime) {
        Map<Field<?>, Object> values = new LinkedHashMap<>();
        values.put(table.field(ADDRESS_UTXO.TX_INDEX), addressUtxo.getTxIndex());
        values.put(table.field(ADDRESS_UTXO.SLOT), addressUtxo.getSlot());
        values.put(table.field(ADDRESS_UTXO.BLOCK_HASH), addressUtxo.getBlockHash());
        values.put(table.field(ADDRESS_UTXO.EPOCH), addressUtxo.getEpoch());
        values.put(table.field(ADDRESS_UTXO.LOVELACE_AMOUNT), addressUtxo.getLovelaceAmount() != null ? addressUtxo.getLovelaceAmount().longValue() : 0L);
        values.put(table.field(ADDRESS_UTXO.AMOUNTS), JSON.valueOf(JsonUtil.getJson(addressUtxo.getAmounts())));
        values.put(table.field(ADDRESS_UTXO.DATA_HASH), addressUtxo.getDataHash());
        values.put(table.field(ADDRESS_UTXO.INLINE_DATUM), addressUtxo.getInlineDatum());
        values.put(table.field(ADDRESS_UTXO.OWNER_ADDR), addressUtxo.getOwnerAddr());
        values.put(table.field(ADDRESS_UTXO.OWNER_ADDR_FULL), addressUtxo.getOwnerAddrFull());
        values.put(table.field(ADDRESS_UTXO.OWNER_STAKE_ADDR), addressUtxo.getOwnerStakeAddr());
        values.put(table.field(ADDRESS_UTXO.OWNER_PAYMENT_CREDENTIAL), addressUtxo.getOwnerPaymentCredential());
        values.put(table.field(ADDRESS_UTXO.OWNER_STAKE_CREDENTIAL), addressUtxo.getOwnerStakeCredential());
        values.put(table.field(ADDRESS_UTXO.SCRIPT_REF), addressUtxo.getScriptRef());
        values.put(table.field(ADDRESS_UTXO.REFERENCE_SCRIPT_HASH), addressUtxo.getReferenceScriptHash());
        values.put(table.field(ADDRESS_UTXO.IS_COLLATERAL_RETURN), addressUtxo.getIsCollateralReturn());
        values.put(table.field(ADDRESS_UTXO.BLOCK), addressUtxo.getBlockNumber());
        values.put(table.field(ADDRESS_UTXO.BLOCK_TIME), addressUtxo.getBlockTime());
        values.put(table.field(ADDRESS_UTXO.UPDATE_DATETIME), localDateTime);

        return dsl.insertInto(table)
                .set(table.field(ADDRESS_UTXO.TX_HASH), addressUtxo.getTxHash())
                .set(table.field(ADDRESS_UTXO.OUTPUT_INDEX), addressUtxo.getOutputIndex())
                .set(values)
                .onDuplicateKeyUpdate()
                .set(values);
    }

    @Override
    @Transactional
    @Plugin(key = PLUGIN_SPENT)
    public void saveSpent(List<TxInput> txInputs) {
        if (txInputs == null || txInputs.isEmpty())
            return;

        boolean addrHash = txInputAddrHash != null && txInputAddrHash.isLive();
        var inserts = txInputs.stream()
                        .map(spentOutput -> {
                            var insert = dsl.insertInto(TX_INPUT)
                                .set(TX_INPUT.TX_HASH, spentOutput.getTxHash())
                                .set(TX_INPUT.OUTPUT_INDEX, spentOutput.getOutputIndex())
                                .set(TX_INPUT.SPENT_AT_SLOT, spentOutput.getSpentAtSlot())
                                .set(TX_INPUT.SPENT_AT_BLOCK, spentOutput.getSpentAtBlock())
                                .set(TX_INPUT.SPENT_AT_BLOCK_HASH, spentOutput.getSpentAtBlockHash())
                                .set(TX_INPUT.SPENT_BLOCK_TIME, spentOutput.getSpentBlockTime())
                                .set(TX_INPUT.SPENT_EPOCH, spentOutput.getSpentEpoch())
                                .set(TX_INPUT.SPENT_TX_HASH, spentOutput.getSpentTxHash());
                            //The hash of the spent output's address, looked up by its key. Null if that output is not
                            //written yet (it is later in the same batch); the commit fills those.
                            if (addrHash)
                                insert = insert.set(TX_INPUT.ADDR_HASH, select(TxInputAddrHash.hash(ADDRESS_UTXO.OWNER_ADDR))
                                        .from(ADDRESS_UTXO)
                                        .where(ADDRESS_UTXO.TX_HASH.eq(spentOutput.getTxHash()))
                                        .and(ADDRESS_UTXO.OUTPUT_INDEX.eq(spentOutput.getOutputIndex())));
                            return insert.onDuplicateKeyIgnore();
                        })
                        .toList();

        dsl.batch(inserts).execute();

        if (addrHash) {
            txInputs.forEach(txInput -> {
                if (txInput.getSpentAtSlot() == null)
                    return;
                addrHashFromSlot.accumulateAndGet(txInput.getSpentAtSlot(), Math::min);
                addrHashToSlot.accumulateAndGet(txInput.getSpentAtSlot(), Math::max);
            });
        } else if (txInputAddrHash != null) {
            //Saved without the hash: if it was kept past these rows, it no longer is
            txInputs.stream().map(TxInput::getSpentAtSlot).filter(Objects::nonNull).min(Long::compare)
                    .ifPresent(txInputAddrHash::savedWithoutHash);
        }

        if (isUnspentTableEnabled()) {
            txInputs.forEach(txInput -> pendingSpentKeys.add(new UtxoKey(txInput.getTxHash(), txInput.getOutputIndex())));
        }
    }

    @Override
    public void handleCommit(CommitEvent commitEvent) {
        try {
            if (isUnspentTableEnabled())
                commitUnspentTable(commitEvent);
            if (txInputAddrHash != null && txInputAddrHash.isLive())
                commitAddrHash(commitEvent);
        } finally {
            utxoCache.clear();
        }
    }

    /**
     * Settles the batch in address_utxo_unspent and records the table as kept to the batch's last block, in one
     * transaction: the outputs the batch did not spend are upserted, and the spent outputs of earlier batches deleted.
     * In sync mode this joins the block's transaction; in parallel mode it runs in its own.
     */
    private void commitUnspentTable(CommitEvent commitEvent) {
        Map<UtxoKey, PendingOutput> outputs = Map.copyOf(pendingOutputs);
        Set<UtxoKey> spentKeys = Set.copyOf(pendingSpentKeys);

        List<PendingOutput> unspent = outputs.entrySet().stream()
                .filter(entry -> !spentKeys.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();

        boolean[] gap = new boolean[1];
        int[] deleted = new int[1];
        commitTransactionTemplate.executeWithoutResult(status -> {
            //Where the last commit left the table. Read it before keptTo below moves it to this batch.
            StoreState kept = unspentUtxoTable.currentState();

            //This batch must start right after that block. If blocks were skipped, the table is switched off.
            if (unspentUtxoTable.stopIfGap(kept, firstBlock(commitEvent))) {
                gap[0] = true;
                return;
            }

            //An output created and spent in this batch is normally not in the table, so there is nothing to delete.
            //But a block can be processed twice: the cursor block after every restart, or a whole batch after a crash
            //between its commit and the cursor update. If the output's block is one the table already holds (at or
            //before the kept slot), the output may be in the table, so delete it. With no state, delete it anyway.
            Long keptSlot = kept != null ? kept.getSlot() : null;
            List<UtxoKey> toDelete = spentKeys.stream()
                    .filter(utxoKey -> !outputs.containsKey(utxoKey)
                            || keptSlot == null
                            || outputs.get(utxoKey).addressUtxo().getSlot() <= keptSlot)
                    .toList();
            deleted[0] = toDelete.size();

            for (int from = 0; from < unspent.size(); from += UNSPENT_WRITE_BATCH_SIZE) {
                dsl.batch(unspent.subList(from, Math.min(from + UNSPENT_WRITE_BATCH_SIZE, unspent.size())).stream()
                        .map(output -> upsert(ADDRESS_UTXO_UNSPENT, mapper.toAddressUtxoEntity(output.addressUtxo()), output.savedAt()))
                        .toList()).execute();
            }

            for (int from = 0; from < toDelete.size(); from += UNSPENT_WRITE_BATCH_SIZE) {
                dsl.batch(toDelete.subList(from, Math.min(from + UNSPENT_WRITE_BATCH_SIZE, toDelete.size())).stream()
                        .map(utxoKey -> dsl.deleteFrom(ADDRESS_UTXO_UNSPENT)
                                .where(ADDRESS_UTXO_UNSPENT.TX_HASH.eq(utxoKey.getTxHash()))
                                .and(ADDRESS_UTXO_UNSPENT.OUTPUT_INDEX.eq(utxoKey.getOutputIndex())))
                        .toList()).execute();
            }

            unspentUtxoTable.keptTo(commitEvent.getMetadata());
        });

        //Only after the transaction succeeded: on failure the batch stays for the next commit
        pendingOutputs.keySet().removeAll(outputs.keySet());
        pendingSpentKeys.removeAll(spentKeys);

        if (gap[0])
            return;

        if (log.isDebugEnabled())
            log.debug("address_utxo_unspent: {} outputs written, {} spent in the same batch, {} spent outputs deleted",
                    unspent.size(), outputs.size() - unspent.size(), deleted[0]);
    }

    /**
     * Fills tx_input.addr_hash for the batch's spends that were saved before the output they spend, and, once the
     * hash is current, records the batch's last slot as how far it is kept. In sync mode this joins the block's
     * transaction; in parallel mode it runs in its own.
     */
    private void commitAddrHash(CommitEvent<?> commitEvent) {
        long from = addrHashFromSlot.get();
        long to = addrHashToSlot.get();
        commitTransactionTemplate.executeWithoutResult(status ->
                txInputAddrHash.committed(from <= to ? from : null, from <= to ? to : null, commitEvent.getMetadata().getSlot()));

        //Only after the transaction succeeded: on failure the range stays for the next commit
        addrHashFromSlot.compareAndSet(from, Long.MAX_VALUE);
        addrHashToSlot.compareAndSet(to, Long.MIN_VALUE);
    }

    /** The lowest block number in the batch, or null if the event carries no blocks. */
    private static Long firstBlock(CommitEvent<?> commitEvent) {
        if (commitEvent.getBlockCaches() == null)
            return null;

        OptionalLong first = commitEvent.getBlockCaches().stream()
                .filter(BatchEvent.class::isInstance)
                .mapToLong(block -> ((BatchEvent) block).getMetadata().getBlock())
                .min();
        return first.isPresent() ? first.getAsLong() : null;
    }

    @Override
    public List<AddressUtxo> getUnspentBySlotGreaterThan(Long slot) {
        var query = dsl
                .select()
                .from(ADDRESS_UTXO)
                .where(ADDRESS_UTXO.SLOT.gt(slot))
                .orderBy(ADDRESS_UTXO.SLOT.asc());

        return query.fetch().into(AddressUtxo.class);
    }

    @Override
    public List<TxInput> getSpentBySlotGreaterThan(Long slot) {
        var query = dsl
                .select()
                .from(TX_INPUT)
                .where(TX_INPUT.SPENT_AT_SLOT.gt(slot))
                .orderBy(TX_INPUT.SPENT_AT_SLOT.asc());

        return query.fetch().into(TxInput.class);
    }

/**   Remove this method after testing
    @EventListener
    @Transactional
    public void handleCommit(CommitEvent event) {
//        try {
//            LocalDateTime localDateTime = LocalDateTime.now();
//            dsl.batched(c -> {
//                for (AddressUtxo addressUtxo : spentUtxoCache) {
//                    c.dsl().insertInto(ADDRESS_UTXO)
//                            .set(ADDRESS_UTXO.TX_HASH, addressUtxo.getTxHash())
//                            .set(ADDRESS_UTXO.OUTPUT_INDEX, addressUtxo.getOutputIndex())
//                            .set(ADDRESS_UTXO.SPENT, true)
//                            .set(ADDRESS_UTXO.SPENT_AT_SLOT, addressUtxo.getSpentAtSlot())
//                            .set(ADDRESS_UTXO.SPENT_AT_BLOCK, addressUtxo.getSpentAtBlock())
//                            .set(ADDRESS_UTXO.SPENT_AT_BLOCK_HASH, addressUtxo.getSpentAtBlockHash())
//                            .set(ADDRESS_UTXO.SPENT_BLOCK_TIME, addressUtxo.getSpentBlockTime())
//                            .set(ADDRESS_UTXO.SPENT_EPOCH, addressUtxo.getSpentEpoch())
//                            .set(ADDRESS_UTXO.SPENT_TX_HASH, addressUtxo.getSpentTxHash())
//                            .set(ADDRESS_UTXO.UPDATE_DATETIME, localDateTime)
//                            .onDuplicateKeyUpdate()
//                            .set(ADDRESS_UTXO.SPENT, true)
//                            .set(ADDRESS_UTXO.SPENT_AT_SLOT, addressUtxo.getSpentAtSlot())
//                            .set(ADDRESS_UTXO.SPENT_AT_BLOCK, addressUtxo.getSpentAtBlock())
//                            .set(ADDRESS_UTXO.SPENT_AT_BLOCK_HASH, addressUtxo.getSpentAtBlockHash())
//                            .set(ADDRESS_UTXO.SPENT_BLOCK_TIME, addressUtxo.getSpentBlockTime())
//                            .set(ADDRESS_UTXO.SPENT_EPOCH, addressUtxo.getSpentEpoch())
//                            .set(ADDRESS_UTXO.SPENT_TX_HASH, addressUtxo.getSpentTxHash())
//                            .set(ADDRESS_UTXO.UPDATE_DATETIME, localDateTime)
//                            .execute();
//                }
//            });
//
//        } finally {
//            spentUtxoCache.clear();
//        }
    }
 **/


}
