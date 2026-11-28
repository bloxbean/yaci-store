package com.bloxbean.cardano.yaci.store.utxo.storage.impl;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.StoreState;
import com.bloxbean.cardano.yaci.store.common.domain.TxInput;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.common.util.JsonUtil;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.common.util.ListUtil;
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
import org.jooq.Record2;
import org.jooq.Row2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.*;
import static org.jooq.impl.DSL.notExists;
import static org.jooq.impl.DSL.row;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.selectOne;
import static org.jooq.impl.DSL.selectFrom;
import static org.jooq.impl.DSL.values;

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
    private static final int PRUNING_DELETE_CHUNK_SIZE = 1000;

    private final UtxoRepository utxoRepository;
    private final TxInputRepository spentOutputRepository;
    private final DSLContext dsl;
    private final UtxoMapper mapper = UtxoMapper.INSTANCE;
    private final UtxoCache utxoCache;
    private final PlatformTransactionManager transactionManager;
    private final UnspentUtxoTable unspentUtxoTable;

    //Outputs saved and keys spent since the last commit, settled in address_utxo_unspent at commit. Each entry's value
    //is the object its save put there, so the save's rollback and the commit remove only that entry, not a later save
    //of the same key (see removeIfSame).
    private final Map<UtxoKey, PendingOutput> pendingOutputs = new ConcurrentHashMap<>();
    private final Map<UtxoKey, Object> pendingSpentKeys = new ConcurrentHashMap<>();

    private record PendingOutput(AddressUtxo addressUtxo, LocalDateTime savedAt) {
    }

    private TransactionTemplate transactionTemplate;
    private TransactionTemplate commitTransactionTemplate;

    @Value("${store.utxo.pruning-batch-size:3000}")
    private int pruningBatchSize = 3000;

    public UtxoStorageImpl(UtxoRepository utxoRepository, TxInputRepository spentOutputRepository, DSLContext dsl,
                           UtxoCache utxoCache, PlatformTransactionManager transactionManager,
                           UnspentUtxoTable unspentUtxoTable) {
        this.utxoRepository = utxoRepository;
        this.spentOutputRepository = spentOutputRepository;
        this.dsl = dsl;
        this.utxoCache = utxoCache;
        this.transactionManager = transactionManager;
        this.unspentUtxoTable = unspentUtxoTable;

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
        //The table lock is held until the rollback commits, so its phase cannot change half way through the
        //rollback, and no background pass interleaves with it. While built and catching up the table follows the
        //rollback too.
        if (unspentUtxoTable != null) {
            unspentUtxoTable.runLockedUntilTransactionEnds(() -> {
                if (!unspentUtxoTable.isKeptOnRollback())
                    return;
                int deleted = dsl.deleteFrom(ADDRESS_UTXO_UNSPENT)
                        .where(ADDRESS_UTXO_UNSPENT.SLOT.gt(slot))
                        .execute();
                log.info("Rollback -- {} address_utxo_unspent records", deleted);
            });
        }

        return utxoRepository.deleteBySlotGreaterThan(slot);
    }

    @Override
    @Transactional
    public int deleteSpentBySlotGreaterThan(Long slot) {
        if (unspentUtxoTable != null) {
            //Decided under the table lock, held until the rollback commits (see deleteUnspentBySlotGreaterThan)
            unspentUtxoTable.runLockedUntilTransactionEnds(() -> {
                if (unspentUtxoTable.isKeptOnRollback()) {
                    //Must run before tx_input is trimmed: the outputs to restore are the ones whose spend is removed
                    int restored = restoreUnspentSpentAfter(slot);
                    log.info("Rollback -- {} address_utxo_unspent records restored", restored);

                    //Outputs and spends not yet committed belong to the blocks being rolled back
                    pendingOutputs.clear();
                    pendingSpentKeys.clear();
                    unspentUtxoTable.rolledBackTo(slot, true);
                } else {
                    unspentUtxoTable.rolledBackTo(slot, false);
                }
            });
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
        //The background catch-up deletes the outputs spent after the build's start from tx_input: keep them until then
        if (unspentUtxoTable != null && unspentUtxoTable.isBuildingOrCatchingUp()) {
            log.info("Pruning of spent outputs skipped while address_utxo_unspent is built");
            return 0;
        }
        int count = 0;
        int totalCount = 0;

        int limit = pruningBatchSize;

        do {
            count = transactionTemplate.execute(status ->  {
                // Fetch the keys once and delete both tables by the same keys. spent_at_block is not unique,
                // so two separate "order by spent_at_block limit N" selections can pick different rows (#1379).
                var spentKeys = dsl.select(TX_INPUT.OUTPUT_INDEX, TX_INPUT.TX_HASH)
                        .from(TX_INPUT)
                        .where(TX_INPUT.SPENT_AT_BLOCK.lt(block))
                        .orderBy(TX_INPUT.SPENT_AT_BLOCK.asc())
                        .limit(limit)
                        .fetch();

                if (spentKeys.isEmpty())
                    return 0;

                int addressUtxoCount = 0;
                int txInputCount = 0;
                // Each key is 2 bind values; delete in chunks to stay under the JDBC/DB bind limits.
                for (var keys : ListUtil.partition(spentKeys.map(Record2::valuesRow), PRUNING_DELETE_CHUNK_SIZE)) {
                    // IN (SELECT ... FROM (VALUES ...)) instead of a row-value IN list: PostgreSQL expands the
                    // latter into a long OR chain, misestimates it and spends seconds in JIT on each delete.
                    var keyTable = selectFrom(values(keys.toArray(new Row2[0])).as("k", "output_index", "tx_hash"));

                    addressUtxoCount += dsl.deleteFrom(ADDRESS_UTXO)
                            .where(row(ADDRESS_UTXO.OUTPUT_INDEX, ADDRESS_UTXO.TX_HASH)
                                    .in(keyTable))
                            .execute();

                    txInputCount += dsl.deleteFrom(TX_INPUT)
                            .where(row(TX_INPUT.OUTPUT_INDEX, TX_INPUT.TX_HASH)
                                    .in(keyTable))
                            .execute();
                }

                if (log.isDebugEnabled())
                    log.debug("Deleted {} address_utxo and {} tx_input records", addressUtxoCount, txInputCount);

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
            List<PendingOutput> genesisOutputs = new ArrayList<>();
            Map<UtxoKey, PendingOutput> added = new LinkedHashMap<>();
            for (AddressUtxo addressUtxo : addressUtxoList) {
                PendingOutput output = new PendingOutput(addressUtxo, localDateTime);
                if (isGenesisOutput(addressUtxo)) {
                    genesisOutputs.add(output);
                } else { //A later save of the same output (pointer-address resolution at commit) replaces it
                    UtxoKey key = new UtxoKey(addressUtxo.getTxHash(), addressUtxo.getOutputIndex());
                    pendingOutputs.put(key, output);
                    added.put(key, output);
                }
            }
            forgetIfRolledBack(() -> added.forEach((key, output) -> removeIfSame(pendingOutputs, key, output)));
            writeGenesisOutputs(genesisOutputs);
        }

        addressUtxoList.forEach(utxoCache::add);
    }

    /** Genesis outputs are saved with slot -1, from the GenesisBlockEvent, before the first block. */
    private static boolean isGenesisOutput(AddressUtxo addressUtxo) {
        return addressUtxo.getSlot() != null && addressUtxo.getSlot() < 0;
    }

    /**
     * Writes the genesis outputs to address_utxo_unspent at once, in the transaction that saves them to address_utxo.
     * They belong to no batch, so they must not wait in the pending outputs for the first commit: a rollback before
     * that commit clears the pending outputs (the RollBackward that chain sync sends to its start point on a fresh
     * store that starts near the tip, or the restart rollback of a fresh store whose first block is after slot 0), and
     * a stop before it loses them, while the rollback has already recorded the table as current. A spend of one of them
     * in the first batch deletes it at that commit, as for any output of an earlier batch.
     */
    private void writeGenesisOutputs(List<PendingOutput> genesisOutputs) {
        if (genesisOutputs.isEmpty())
            return;

        //Saved again into a store that has already spent some of them: leave those out
        List<PendingOutput> toWrite = withoutLaterSpends(genesisOutputs, -1L);
        for (int from = 0; from < toWrite.size(); from += UNSPENT_WRITE_BATCH_SIZE) {
            dsl.batch(toWrite.subList(from, Math.min(from + UNSPENT_WRITE_BATCH_SIZE, toWrite.size())).stream()
                    .map(output -> upsert(ADDRESS_UTXO_UNSPENT, mapper.toAddressUtxoEntity(output.addressUtxo()), output.savedAt()))
                    .toList()).execute();
        }
        log.info("address_utxo_unspent: {} genesis outputs written", toWrite.size());
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

        var inserts = txInputs.stream()
                        .map(spentOutput -> dsl.insertInto(TX_INPUT)
                                .set(TX_INPUT.TX_HASH, spentOutput.getTxHash())
                                .set(TX_INPUT.OUTPUT_INDEX, spentOutput.getOutputIndex())
                                .set(TX_INPUT.SPENT_AT_SLOT, spentOutput.getSpentAtSlot())
                                .set(TX_INPUT.SPENT_AT_BLOCK, spentOutput.getSpentAtBlock())
                                .set(TX_INPUT.SPENT_AT_BLOCK_HASH, spentOutput.getSpentAtBlockHash())
                                .set(TX_INPUT.SPENT_BLOCK_TIME, spentOutput.getSpentBlockTime())
                                .set(TX_INPUT.SPENT_EPOCH, spentOutput.getSpentEpoch())
                                .set(TX_INPUT.SPENT_TX_HASH, spentOutput.getSpentTxHash())
                                .onDuplicateKeyIgnore())
                                .toList();

        dsl.batch(inserts).execute();

        if (isUnspentTableEnabled()) {
            Object token = new Object();
            List<UtxoKey> keys = txInputs.stream()
                    .map(txInput -> new UtxoKey(txInput.getTxHash(), txInput.getOutputIndex()))
                    .toList();
            keys.forEach(key -> pendingSpentKeys.put(key, token));
            forgetIfRolledBack(() -> keys.forEach(key -> removeIfSame(pendingSpentKeys, key, token)));
        }
    }

    /**
     * The pending outputs and spends must not outlive the transaction that saved them. If it rolls back (a listener of
     * the same block throws, a sync session that was replaced is stopped mid-block, ...), address_utxo and tx_input lose
     * those rows, and the next commit would write outputs that are not in address_utxo or delete outputs that are still
     * unspent. A RollbackEvent clears them too, but not every such failure is followed by one before the next commit.
     */
    private static void forgetIfRolledBack(Runnable forget) {
        if (!org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive())
            return;
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status == STATUS_ROLLED_BACK)
                            forget.run();
                    }
                });
    }

    /** Removes the entry only if it is still the one put there (by identity), not a later save of the same key. */
    private static <V> void removeIfSame(Map<UtxoKey, V> map, UtxoKey key, V value) {
        map.computeIfPresent(key, (k, current) -> current == value ? null : current);
    }

    @Override
    public void handleCommit(CommitEvent commitEvent) {
        try {
            if (isUnspentTableEnabled()) {
                commitUnspentTable(commitEvent);
            } else if (unspentUtxoTable != null && unspentUtxoTable.isCatchingUp()) {
                //A table built in the background is switched on at a commit, in the commit's transaction. Only
                //checked while it is catching up, so a store without the table opens no transaction per commit.
                commitTransactionTemplate.executeWithoutResult(status ->
                        unspentUtxoTable.switchOnAtCommit(commitEvent.getMetadata()));
            }
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
        Map<UtxoKey, Object> spent = Map.copyOf(pendingSpentKeys);
        Set<UtxoKey> spentKeys = spent.keySet();

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
                            || outputs.get(utxoKey).addressUtxo().getSlot() == null
                            || outputs.get(utxoKey).addressUtxo().getSlot() <= keptSlot)
                    .toList();
            deleted[0] = toDelete.size();

            //A block processed again (at or before the kept slot) can carry outputs that a later block, already in
            //the table, spent. They must not be written back (#1328 J07: a hung listener finished an old block late).
            List<PendingOutput> toWrite = withoutLaterSpends(unspent, keptSlot);

            for (int from = 0; from < toWrite.size(); from += UNSPENT_WRITE_BATCH_SIZE) {
                dsl.batch(toWrite.subList(from, Math.min(from + UNSPENT_WRITE_BATCH_SIZE, toWrite.size())).stream()
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

            //A block finished late (older than what the table holds) must not move the state back
            EventMetadata metadata = commitEvent.getMetadata();
            if (kept == null || kept.getBlock() == null || metadata.getBlock() >= kept.getBlock())
                unspentUtxoTable.keptTo(metadata);
        });

        //Parallel mode: the commit has its own transaction, so if it fails the batch stays for the next commit (its
        //blocks are already committed). Sync mode: this joined the block's transaction, which can still roll back; then
        //the block's address_utxo and tx_input rows go too, and the block is processed again from the cursor, so the
        //batch is saved again. Only the entries settled here are removed: a save of the same key since the copy above
        //stays for the next commit.
        outputs.forEach((key, output) -> removeIfSame(pendingOutputs, key, output));
        spent.forEach((key, token) -> removeIfSame(pendingSpentKeys, key, token));

        if (gap[0]) {
            //After the commit that marked the table stale: a build started earlier would conflict with that update
            runAfterCommit(() -> unspentUtxoTable.startBuild("blocks were missed"));
            return;
        }

        if (log.isDebugEnabled())
            log.debug("address_utxo_unspent: {} outputs written, {} spent in the same batch, {} spent outputs deleted",
                    unspent.size(), outputs.size() - unspent.size(), deleted[0]);
    }

    /**
     * Drops the outputs of blocks the table already holds (slot at or before {@code keptSlot}) that have a spend in
     * tx_input. New blocks are not looked up, so the normal path costs nothing.
     */
    private List<PendingOutput> withoutLaterSpends(List<PendingOutput> outputs, Long keptSlot) {
        if (keptSlot == null)
            return outputs;

        List<PendingOutput> replayed = outputs.stream()
                .filter(output -> output.addressUtxo().getSlot() != null && output.addressUtxo().getSlot() <= keptSlot)
                .toList();
        if (replayed.isEmpty())
            return outputs;

        Set<UtxoKey> spent = new HashSet<>();
        for (int from = 0; from < replayed.size(); from += UNSPENT_WRITE_BATCH_SIZE) {
            var keys = replayed.subList(from, Math.min(from + UNSPENT_WRITE_BATCH_SIZE, replayed.size())).stream()
                    .map(output -> row(output.addressUtxo().getTxHash(), output.addressUtxo().getOutputIndex()))
                    .toList();
            dsl.select(TX_INPUT.TX_HASH, TX_INPUT.OUTPUT_INDEX)
                    .from(TX_INPUT)
                    .where(row(TX_INPUT.TX_HASH, TX_INPUT.OUTPUT_INDEX).in(keys))
                    .fetch()
                    .forEach(r -> spent.add(new UtxoKey(r.value1(), r.value2())));
        }
        if (spent.isEmpty())
            return outputs;

        log.warn("address_utxo_unspent: not writing back {} outputs of blocks processed again, they are spent already",
                spent.size());
        return outputs.stream()
                .filter(output -> !spent.contains(new UtxoKey(output.addressUtxo().getTxHash(), output.addressUtxo().getOutputIndex())))
                .toList();
    }

    private static void runAfterCommit(Runnable work) {
        if (!org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            work.run();
            return;
        }
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        work.run();
                    }
                });
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
