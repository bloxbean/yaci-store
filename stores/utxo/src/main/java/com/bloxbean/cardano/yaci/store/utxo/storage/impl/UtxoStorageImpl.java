package com.bloxbean.cardano.yaci.store.utxo.storage.impl;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.TxInput;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.common.util.JsonUtil;
import com.bloxbean.cardano.yaci.store.events.internal.CommitEvent;
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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
 * and genesis outputs apply to both tables alike:
 * <ul>
 *   <li>{@link #saveUnspent} upserts each output into both tables in the same transaction, which
 *   in parallel mode is the block's own transaction, so nothing is read back;</li>
 *   <li>{@link #saveSpent} only remembers the spent keys. They are deleted from
 *   address_utxo_unspent at {@link #handleCommit}, when every output of the batch has been written
 *   (in parallel mode a block's spends can be saved before the output they spend, which another
 *   block of the same batch writes, and {@code UtxoProcessor} saves a block's spends before its
 *   outputs);</li>
 *   <li>a rollback deletes the rows created after the rollback point and restores the outputs
 *   whose spend it removes, before tx_input is trimmed.</li>
 * </ul>
 */
@Slf4j
public class UtxoStorageImpl implements UtxoStorage {
    private static final String PLUGIN_UNSPENT = "utxo.unspent.save";
    private static final String PLUGIN_SPENT = "utxo.spent.save";

    private final UtxoRepository utxoRepository;
    private final TxInputRepository spentOutputRepository;
    private final DSLContext dsl;
    private final UtxoMapper mapper = UtxoMapper.INSTANCE;
    private final UtxoCache utxoCache;
    private final PlatformTransactionManager transactionManager;
    private final UnspentUtxoTable unspentUtxoTable;

    //Keys spent since the last commit, deleted from address_utxo_unspent at commit
    private final Set<UtxoKey> pendingSpentKeys = ConcurrentHashMap.newKeySet();

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

            //Spends not yet committed belong to the blocks being rolled back
            pendingSpentKeys.clear();
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
            var unspentInserts = addressUtxoEntities.stream()
                    .map(addressUtxo -> upsert(ADDRESS_UTXO_UNSPENT, addressUtxo, localDateTime))
                    .toList();

            dsl.batch(unspentInserts).execute();
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
            txInputs.forEach(txInput -> pendingSpentKeys.add(new UtxoKey(txInput.getTxHash(), txInput.getOutputIndex())));
        }
    }

    @Override
    public void handleCommit(CommitEvent commitEvent) {
        try {
            if (isUnspentTableEnabled())
                deletePendingSpentFromUnspentTable();
        } finally {
            utxoCache.clear();
        }
    }

    /**
     * Deletes the outputs spent since the last commit from address_utxo_unspent. By now every output of the
     * batch is written, so each spent output is found whichever block of the batch wrote it.
     * In sync mode this joins the block's transaction; in parallel mode it runs in its own.
     */
    private void deletePendingSpentFromUnspentTable() {
        if (pendingSpentKeys.isEmpty())
            return;

        List<UtxoKey> spentKeys = List.copyOf(pendingSpentKeys);
        commitTransactionTemplate.executeWithoutResult(status -> {
            var deletes = spentKeys.stream()
                    .map(utxoKey -> dsl.deleteFrom(ADDRESS_UTXO_UNSPENT)
                            .where(ADDRESS_UTXO_UNSPENT.TX_HASH.eq(utxoKey.getTxHash()))
                            .and(ADDRESS_UTXO_UNSPENT.OUTPUT_INDEX.eq(utxoKey.getOutputIndex())))
                    .toList();

            dsl.batch(deletes).execute();
        });

        //Only after the delete succeeded: on failure the keys stay for the next commit
        pendingSpentKeys.removeAll(spentKeys);

        if (log.isDebugEnabled())
            log.debug("Deleted {} spent outputs from address_utxo_unspent", spentKeys.size());
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
