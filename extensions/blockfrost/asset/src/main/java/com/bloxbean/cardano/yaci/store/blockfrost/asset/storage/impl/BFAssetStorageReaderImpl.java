package com.bloxbean.cardano.yaci.store.blockfrost.asset.storage.impl;

import com.bloxbean.cardano.yaci.store.blockfrost.asset.storage.BFAssetStorageReader;
import com.bloxbean.cardano.yaci.store.blockfrost.asset.storage.impl.model.BFAssetAddress;
import com.bloxbean.cardano.yaci.store.blockfrost.asset.storage.impl.model.BFAssetHistory;
import com.bloxbean.cardano.yaci.store.blockfrost.asset.storage.impl.model.BFAssetInfo;
import com.bloxbean.cardano.yaci.store.blockfrost.asset.storage.impl.model.BFAssetTransaction;
import com.bloxbean.cardano.yaci.store.blockfrost.asset.storage.impl.model.BFPolicyAsset;
import com.bloxbean.cardano.yaci.store.blockfrost.common.util.AmountsJsonUtil;
import com.bloxbean.cardano.yaci.store.blockfrost.common.util.BlockfrostDialectUtil;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.Row2;
import org.jooq.SortField;
import org.jooq.Table;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.store.assets.jooq.Tables.ASSETS;
import static com.bloxbean.cardano.yaci.store.transaction.jooq.Tables.TRANSACTION;
import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.ADDRESS_UTXO;
import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.TX_INPUT;

@Component
@RequiredArgsConstructor
@Slf4j
public class BFAssetStorageReaderImpl implements BFAssetStorageReader {
    private final DSLContext dsl;

    /**
     * Returns one page of the asset list, each unit with its first-mint tx and total quantity.
     */
    @Override
    public List<BFPolicyAsset> findAssets(int page, int count, Order order) {
        int offset = Math.max(page, 0) * count;
        List<FirstSeenUnit> pageRows = findFirstSeenUnitsPage(offset, count, order);
        if (pageRows.isEmpty()) {
            return List.of();
        }

        List<String> pageUnits = pageRows.stream()
                .map(FirstSeenUnit::unit)
                .collect(Collectors.toList());

        // Phase 2: one aggregate query for current page units (no N+1, no UNION ALL)
        // Use (unit, policy) predicates to align with unit-first composite index.
        Map<String, String> policyByUnit = pageUnits.stream()
                .collect(Collectors.toMap(
                        unit -> unit,
                        this::extractPolicy,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        List<Row2<String, String>> unitPolicyPairs = policyByUnit.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .map(entry -> DSL.row(entry.getKey(), entry.getValue()))
                .toList();
        List<String> unitsWithoutPolicy = policyByUnit.entrySet().stream()
                .filter(entry -> entry.getValue() == null)
                .map(Map.Entry::getKey)
                .toList();

        Field<String> quantityUnitField = ASSETS.UNIT.as("unit");
        Field<BigDecimal> quantityField = DSL.sum(ASSETS.QUANTITY.cast(BigDecimal.class)).as("quantity");
        Condition pageUnitsCondition = DSL.falseCondition();
        if (!unitPolicyPairs.isEmpty()) {
            pageUnitsCondition = pageUnitsCondition.or(DSL.row(ASSETS.UNIT, ASSETS.POLICY).in(unitPolicyPairs));
        }
        if (!unitsWithoutPolicy.isEmpty()) {
            pageUnitsCondition = pageUnitsCondition.or(ASSETS.UNIT.in(unitsWithoutPolicy));
        }

        var quantityQuery = dsl.select(quantityUnitField, quantityField)
                .from(ASSETS)
                .where(pageUnitsCondition)
                .groupBy(ASSETS.UNIT);

        logQuery("findAssetsQuantities", quantityQuery);
        Map<String, BigInteger> quantityByUnit = quantityQuery.fetch().stream()
                .collect(Collectors.toMap(
                        record -> record.get(quantityUnitField),
                        record -> toBigInteger(record.get(quantityField))
                ));

        return pageRows.stream()
                .map(row -> new BFPolicyAsset(
                        row.unit(),
                        quantityByUnit.getOrDefault(row.unit(), BigInteger.ZERO),
                        row.slot(),
                        row.txHash()
                ))
                .toList();
    }

    /**
     * Returns the full details for a single asset unit, or empty if it was never minted.
     */
    @Override
    public Optional<BFAssetInfo> findAssetInfo(String unit) {
        Condition assetCondition = buildAssetCondition(unit);

        var initialMintTxQuery = dsl.select(ASSETS.TX_HASH)
                .from(ASSETS)
                .where(assetCondition)
                .orderBy(ASSETS.SLOT.asc().nullsLast(), ASSETS.TX_HASH.asc())
                .limit(1);
        logQuery("findAssetInfoInitialMintTx", initialMintTxQuery);

        String initialMintTxHash = initialMintTxQuery.fetchOne(ASSETS.TX_HASH);
        if (initialMintTxHash == null) {
            return Optional.empty();
        }

        Field<String> outPolicyField = DSL.min(ASSETS.POLICY).as("policy_id");
        Field<String> outAssetNameField = DSL.min(ASSETS.ASSET_NAME).as("asset_name");
        Field<String> outFingerprintField = DSL.min(ASSETS.FINGERPRINT).as("fingerprint");
        Field<BigDecimal> outQuantityField = DSL.sum(ASSETS.QUANTITY.cast(BigDecimal.class)).as("quantity");
        Field<Long> outMintBurnCountField = DSL.count().cast(Long.class).as("mint_or_burn_count");

        var aggregateQuery = dsl.select(
                        outPolicyField,
                        outAssetNameField,
                        outFingerprintField,
                        outQuantityField,
                        outMintBurnCountField
                )
                .from(ASSETS)
                .where(assetCondition);

        logQuery("findAssetInfoAggregate", aggregateQuery);
        var result = aggregateQuery.fetchOne();

        if (result == null || result.get(outMintBurnCountField) == null || result.get(outMintBurnCountField) == 0L) {
            return Optional.empty();
        }

        return Optional.of(new BFAssetInfo(
                unit,
                result.get(outPolicyField),
                result.get(outAssetNameField),
                result.get(outFingerprintField),
                toBigInteger(result.get(outQuantityField)),
                initialMintTxHash,
                result.get(outMintBurnCountField)
        ));
    }

    /**
     * Returns one page of an asset's mint/burn history, ordered to match Blockfrost.
     */
    @Override
    public List<BFAssetHistory> findAssetHistory(String unit, int page, int count, Order order) {
        if (BlockfrostDialectUtil.isPostgres(dsl)) {
            return findAssetHistoryPostgres(unit, page, count, order);
        }
        return findAssetHistoryFallback(unit, page, count, order);
    }

    private List<BFAssetHistory> findAssetHistoryPostgres(String unit, int page, int count, Order order) {
        int offset = Math.max(page, 0) * count;
        String slotOrder = order == Order.desc ? "desc nulls last" : "asc nulls last";
        String txIndexOrder = order == Order.desc ? "desc nulls last" : "asc nulls first";
        String txHashOrder = order == Order.desc ? "desc" : "asc";
        String boundaryStartAgg = order == Order.desc ? "max" : "min";
        String boundaryEndAgg = order == Order.desc ? "min" : "max";
        String prefixCmp = order == Order.desc ? ">" : "<";
        String candLo = order == Order.desc ? "<=" : ">=";
        String candHi = order == Order.desc ? ">=" : "<=";

        Table<?> historyPage = DSL.table(
                """
                (
                    with events as materialized (
                        select tx_hash, mint_type, quantity, slot::bigint as slot
                        from assets
                        where unit = {2}
                    ),
                    page_window as materialized (
                        select slot, tx_hash
                        from events
                        order by slot %s, tx_hash %s
                        offset {0} rows fetch next {1} rows only
                    ),
                    boundary as (
                        select %s(slot) as start_slot, %s(slot) as end_slot
                        from page_window
                    ),
                    prefix as (
                        select count(*) as cnt
                        from events
                        where slot %s (select start_slot from boundary)
                    ),
                    candidates as (
                        select e.tx_hash, e.mint_type, e.quantity, e.slot, t.tx_index
                        from events e
                        join transaction t on t.tx_hash = e.tx_hash, boundary b
                        where e.slot %s b.start_slot
                          and e.slot %s b.end_slot
                    ),
                    ranked as (
                        select tx_hash, mint_type, quantity,
                               row_number() over (order by slot %s, tx_index %s, tx_hash %s) as rn
                        from candidates
                    )
                    select tx_hash, mint_type, quantity
                    from ranked
                    where rn > ({0} - (select cnt from prefix))
                      and rn <= ({0} - (select cnt from prefix) + {1})
                    order by rn
                ) as history_page
                """.formatted(
                        slotOrder, txHashOrder,
                        boundaryStartAgg, boundaryEndAgg,
                        prefixCmp,
                        candLo, candHi,
                        slotOrder, txIndexOrder, txHashOrder
                ),
                DSL.inline(offset), DSL.inline(count), DSL.val(unit)
        );

        Field<String> txHashField = DSL.field(DSL.name("history_page", "tx_hash"), String.class);
        Field<String> mintTypeField = DSL.field(DSL.name("history_page", "mint_type"), String.class);
        Field<BigInteger> quantityField = DSL.field(DSL.name("history_page", "quantity"), BigInteger.class);

        var query = dsl.select(txHashField, mintTypeField, quantityField).from(historyPage);
        logQuery("findAssetHistoryPostgres", query);
        return query.fetch(record -> toHistory(
                record.get(txHashField),
                record.get(mintTypeField),
                record.get(quantityField)
        ));
    }

    private List<BFAssetHistory> findAssetHistoryFallback(String unit, int page, int count, Order order) {
        int offset = Math.max(page, 0) * count;
        Condition assetCondition = buildAssetCondition(unit);
        SortField<?> slotOrder = order == Order.desc ? ASSETS.SLOT.desc().nullsLast() : ASSETS.SLOT.asc().nullsLast();
        SortField<?> txIndexOrder = order == Order.desc
                ? TRANSACTION.TX_INDEX.desc().nullsLast()
                : TRANSACTION.TX_INDEX.asc().nullsFirst();
        SortField<?> txOrder = order == Order.desc ? ASSETS.TX_HASH.desc() : ASSETS.TX_HASH.asc();

        var query = dsl.select(ASSETS.TX_HASH, ASSETS.MINT_TYPE, ASSETS.QUANTITY)
                .from(ASSETS)
                .join(TRANSACTION).on(TRANSACTION.TX_HASH.eq(ASSETS.TX_HASH))
                .where(assetCondition)
                .orderBy(slotOrder, txIndexOrder, txOrder)
                .limit(count)
                .offset(offset);

        logQuery("findAssetHistoryFallback", query);
        return query.fetch(record -> toHistory(
                record.get(ASSETS.TX_HASH),
                record.get(ASSETS.MINT_TYPE),
                record.get(ASSETS.QUANTITY)
        ));
    }

    /**
     * Builds a history entry from a mint/burn row.
     */
    private BFAssetHistory toHistory(String txHash, String mintType, Object quantity) {
        String action = "BURN".equalsIgnoreCase(mintType) ? "burned" : "minted";
        return new BFAssetHistory(txHash, action, toBigInteger(quantity));
    }

    /**
     * Returns transaction hashes related to the provided unit, ordered by
     * (block, tx_index, tx_hash) to match Blockfrost.
     */
    @Override
    public List<String> findAssetTxHashes(String unit, int page, int count, Order order) {
        return assetTxPage(unit, page, count, order).stream()
                .map(AssetTxRow::txHash)
                .toList();
    }

    /**
     * Returns transaction references for the provided unit with block height/time and tx index,
     * ordered by (block, tx_index, tx_hash) to match Blockfrost.
     */
    @Override
    public List<BFAssetTransaction> findAssetTransactions(String unit, int page, int count, Order order) {
        return assetTxPage(unit, page, count, order).stream()
                .map(row -> new BFAssetTransaction(row.txHash(), row.txIndex(), row.block(), row.blockTime()))
                .toList();
    }

    /**
     * Returns current holder addresses for a unit with quantity and first-seen slot.
     * Only unspent UTXO rows are considered and quantity is extracted from the amounts JSON array.
     * Non-Postgres dialects (for example H2/MySQL) use an application-side JSON parsing fallback.
     */
    @Override
    public List<BFAssetAddress> findAssetAddresses(String unit, int page, int count, Order order) {
        if (BlockfrostDialectUtil.isPostgres(dsl)) {
            return findAssetAddressesPostgres(unit, page, count, order);
        }
        return findAssetAddressesNonPostgres(unit, page, count, order);
    }

    /**
     * Returns current holders in Blockfrost's order, which isn't symmetric between asc and desc. For asc
     * we order each holder by its earliest unspent UTXO (slot, tx_index, output_index); for desc we order
     * by its latest UTXO instead. Since the two directions look at different rows, desc is not simply asc
     * reversed. Address is the final tie-break.
     *
     * <p>This still scans and aggregates every holder UTXO, so it's slow on units with many holders.
     */
    private List<BFAssetAddress> findAssetAddressesPostgres(String unit, int page, int count, Order order) {
        int offset = Math.max(page, 0) * count;
        String dir = order == Order.desc ? "desc" : "asc";
        String unitJson = "[{\"unit\": \"" + unit + "\"}]";

        Table<?> assetAddresses = DSL.table(
                """
                (
                    with cand as (
                        select coalesce(au.owner_addr_full, au.owner_addr) as address,
                               au.slot as slot, t.tx_index as tx_index, au.output_index as output_index,
                               (
                                   select (elem->>'quantity')::numeric
                                   from jsonb_array_elements(au.amounts) elem
                                   where elem->>'unit' = {2}
                                   limit 1
                               ) as quantity
                        from address_utxo au
                        join transaction t on t.tx_hash = au.tx_hash
                        where au.amounts @> {3}::jsonb
                          and not exists (
                              select 1 from tx_input ti
                              where ti.tx_hash = au.tx_hash and ti.output_index = au.output_index
                          )
                    ),
                    keyed as (
                        select distinct on (address)
                               address, slot as k_slot, tx_index as k_tx_index, output_index as k_output_index
                        from cand
                        order by address, slot %1$s, tx_index %1$s, output_index %1$s
                    ),
                    agg as (
                        select address, sum(quantity) as quantity
                        from cand
                        group by address
                    )
                    select k.address as address, a.quantity as quantity, k.k_slot as first_seen_slot
                    from keyed k
                    join agg a on a.address = k.address
                    order by k.k_slot %1$s, k.k_tx_index %1$s, k.k_output_index %1$s, k.address %1$s
                    offset {0} rows fetch next {1} rows only
                ) as asset_addresses
                """.formatted(dir),
                DSL.inline(offset), DSL.inline(count), DSL.val(unit), DSL.val(unitJson)
        );

        Field<String> addressField = DSL.field(DSL.name("asset_addresses", "address"), String.class);
        Field<BigDecimal> quantityField = DSL.field(DSL.name("asset_addresses", "quantity"), BigDecimal.class);
        Field<Long> firstSeenSlotField = DSL.field(DSL.name("asset_addresses", "first_seen_slot"), Long.class);

        var query = dsl.select(addressField, quantityField, firstSeenSlotField).from(assetAddresses);
        logQuery("findAssetAddresses", query);

        return query.fetch(record -> new BFAssetAddress(
                        record.get(addressField),
                        toBigInteger(record.get(quantityField)),
                        record.get(firstSeenSlotField)
                ));
    }

    private List<BFAssetAddress> findAssetAddressesNonPostgres(String unit, int page, int count, Order order) {
        int offset = Math.max(page, 0) * count;
        Field<String> ownerAddrField = ADDRESS_UTXO.OWNER_ADDR.as("owner_addr");
        Field<String> ownerAddrFullField = ADDRESS_UTXO.OWNER_ADDR_FULL.as("owner_addr_full");
        Field<Long> slotField = ADDRESS_UTXO.SLOT.as("slot");
        Field<String> amountsField = ADDRESS_UTXO.AMOUNTS.cast(String.class).as("amounts");

        var rows = dsl.select(ownerAddrField, ownerAddrFullField, slotField, amountsField)
                .from(ADDRESS_UTXO)
                .where(buildUnitCondition(unit))
                .andNotExists(
                        dsl.selectOne()
                                .from(TX_INPUT)
                                .where(TX_INPUT.TX_HASH.eq(ADDRESS_UTXO.TX_HASH))
                                .and(TX_INPUT.OUTPUT_INDEX.eq(ADDRESS_UTXO.OUTPUT_INDEX))
                )
                .fetch();

        Map<String, HolderAggregate> byAddress = new HashMap<>();
        for (var row : rows) {
            String address = row.get(ownerAddrFullField);
            if (address == null || address.isBlank()) {
                address = row.get(ownerAddrField);
            }
            if (address == null || address.isBlank()) {
                continue;
            }

            BigInteger quantity = AmountsJsonUtil.findQuantity(row.get(amountsField), unit);
            if (quantity == null) {
                continue;
            }

            Long slot = row.get(slotField);
            byAddress.compute(address, (key, aggregate) -> {
                if (aggregate == null) {
                    return new HolderAggregate(quantity, slot);
                }
                return aggregate.merge(quantity, slot);
            });
        }

        Comparator<BFAssetAddress> comparator = Comparator
                .comparing(BFAssetAddress::firstSeenSlot, slotComparator(order))
                .thenComparing(BFAssetAddress::address, order == Order.desc ? Comparator.reverseOrder() : Comparator.naturalOrder());

        List<BFAssetAddress> sorted = byAddress.entrySet().stream()
                .map(entry -> new BFAssetAddress(entry.getKey(), entry.getValue().quantity, entry.getValue().firstSeenSlot))
                .sorted(comparator)
                .toList();

        if (offset >= sorted.size()) {
            return List.of();
        }

        int toIndex = Math.min(offset + count, sorted.size());
        return sorted.subList(offset, toIndex);
    }

    /**
     * Returns the assets under a policy, ordered by each unit's first mint: slot, then tx_index, then unit.
     *
     * <p>We pick each unit's first mint by (slot, tx_index, tx_hash), since Blockfrost breaks same-block
     * ties by tx_index rather than tx_hash (same idea as {@link #findFirstSeenUnitsPagePostgres}, the
     * {@code /assets} list). The quantity is the current supply, summed over every mint and burn for the unit.
     */
    @Override
    public List<BFPolicyAsset> findAssetsByPolicy(String policyId, int page, int count, Order order) {
        int offset = Math.max(page, 0) * count;

        Field<Integer> rnField = DSL.rowNumber()
                .over(DSL.partitionBy(ASSETS.UNIT)
                        .orderBy(ASSETS.SLOT.asc().nullsLast(), TRANSACTION.TX_INDEX.asc().nullsFirst(), ASSETS.TX_HASH.asc()))
                .as("rn");

        Table<?> rankedAssets = dsl.select(
                        ASSETS.UNIT.as("unit"),
                        ASSETS.SLOT.as("slot"),
                        ASSETS.TX_HASH.as("tx_hash"),
                        TRANSACTION.TX_INDEX.as("tx_index"),
                        rnField
                )
                .from(ASSETS)
                .join(TRANSACTION).on(TRANSACTION.TX_HASH.eq(ASSETS.TX_HASH))
                .where(ASSETS.POLICY.eq(policyId).and(ASSETS.MINT_TYPE.eq("MINT")))
                .asTable("ranked_assets");

        Field<String> rankedUnitField = rankedAssets.field("unit", String.class);
        Field<Long> rankedSlotField = rankedAssets.field("slot", Long.class);
        Field<String> rankedTxHashField = rankedAssets.field("tx_hash", String.class);
        Field<Integer> rankedTxIndexField = rankedAssets.field("tx_index", Integer.class);
        Field<Integer> rankedRnField = rankedAssets.field("rn", Integer.class);

        Table<?> firstSeenAssets = dsl.select(rankedUnitField, rankedSlotField, rankedTxHashField, rankedTxIndexField)
                .from(rankedAssets)
                .where(rankedRnField.eq(1))
                .asTable("first_seen_assets");

        Table<?> aggregatedQuantities = dsl.select(
                        ASSETS.UNIT.as("unit"),
                        DSL.sum(ASSETS.QUANTITY.cast(BigDecimal.class)).as("quantity")
                )
                .from(ASSETS)
                .where(ASSETS.POLICY.eq(policyId))
                .groupBy(ASSETS.UNIT)
                .asTable("aggregated_quantities");

        Field<String> unitField = firstSeenAssets.field("unit", String.class);
        Field<Long> slotField = firstSeenAssets.field("slot", Long.class);
        Field<String> txHashField = firstSeenAssets.field("tx_hash", String.class);
        Field<Integer> txIndexField = firstSeenAssets.field("tx_index", Integer.class);
        Field<String> quantityUnitField = aggregatedQuantities.field("unit", String.class);
        Field<BigDecimal> quantityField = aggregatedQuantities.field("quantity", BigDecimal.class);

        SortField<?> slotOrder = order == Order.desc ? slotField.desc().nullsLast() : slotField.asc().nullsLast();
        SortField<?> txIndexOrder = order == Order.desc ? txIndexField.desc().nullsLast() : txIndexField.asc().nullsFirst();
        SortField<?> unitOrder = order == Order.desc ? unitField.desc() : unitField.asc();

        var query = dsl.select(unitField, quantityField, slotField, txHashField)
                .from(firstSeenAssets)
                .leftJoin(aggregatedQuantities)
                .on(quantityUnitField.eq(unitField))
                .orderBy(slotOrder, txIndexOrder, unitOrder)
                .limit(count)
                .offset(offset);

        logQuery("findAssetsByPolicy", query);

        return query.fetch(record -> new BFPolicyAsset(
                        record.get(unitField),
                        toBigInteger(record.get(quantityField)),
                        record.get(slotField),
                        record.get(txHashField)
                ));
    }

    /**
     * Returns one page of a unit's transactions, ordered to match Blockfrost.
     */
    private List<AssetTxRow> assetTxPage(String unit, int page, int count, Order order) {
        if (BlockfrostDialectUtil.isPostgres(dsl)) {
            return assetTxPagePostgres(unit, page, count, order);
        }
        return assetTxPageFallback(unit, page, count, order);
    }

    private List<AssetTxRow> assetTxPagePostgres(String unit, int page, int count, Order order) {
        int offset = Math.max(page, 0) * count;
        String slotOrder = order == Order.desc ? "desc nulls last" : "asc nulls last";
        String txIndexOrder = order == Order.desc ? "desc nulls last" : "asc nulls first";
        String txHashOrder = order == Order.desc ? "desc" : "asc";
        String boundaryStartAgg = order == Order.desc ? "max" : "min";
        String boundaryEndAgg = order == Order.desc ? "min" : "max";
        String prefixCmp = order == Order.desc ? ">" : "<";
        String candLo = order == Order.desc ? "<=" : ">=";
        String candHi = order == Order.desc ? ">=" : "<=";

        String unitJson = "[{\"unit\": \"" + unit + "\"}]";

        Table<?> assetTxPage = DSL.table(
                """
                (
                    with distinct_tx as materialized (
                        select tx_hash, min(slot) as slot
                        from address_utxo
                        where amounts @> {2}::jsonb
                        group by tx_hash
                    ),
                    page_window as materialized (
                        select tx_hash, slot
                        from distinct_tx
                        order by slot %s, tx_hash %s
                        offset {0} rows fetch next {1} rows only
                    ),
                    boundary as (
                        select %s(slot) as start_slot, %s(slot) as end_slot
                        from page_window
                    ),
                    prefix as (
                        select count(*) as cnt
                        from distinct_tx
                        where slot %s (select start_slot from boundary)
                    ),
                    candidates as (
                        select dt.tx_hash, dt.slot, t.tx_index, t.block, t.block_time
                        from distinct_tx dt
                        join transaction t on t.tx_hash = dt.tx_hash, boundary b
                        where dt.slot %s b.start_slot
                          and dt.slot %s b.end_slot
                    ),
                    ranked as (
                        select tx_hash, tx_index, block, block_time,
                               row_number() over (order by slot %s, tx_index %s, tx_hash %s) as rn
                        from candidates
                    )
                    select tx_hash, coalesce(tx_index, 0) as tx_index, block, block_time
                    from ranked
                    where rn > ({0} - (select cnt from prefix))
                      and rn <= ({0} - (select cnt from prefix) + {1})
                    order by rn
                ) as asset_tx_page
                """.formatted(
                        slotOrder, txHashOrder,
                        boundaryStartAgg, boundaryEndAgg,
                        prefixCmp,
                        candLo, candHi,
                        slotOrder, txIndexOrder, txHashOrder
                ),
                DSL.inline(offset), DSL.inline(count), DSL.val(unitJson)
        );

        Field<String> txHashField = DSL.field(DSL.name("asset_tx_page", "tx_hash"), String.class);
        Field<Long> txIndexField = DSL.field(DSL.name("asset_tx_page", "tx_index"), Long.class);
        Field<Long> blockField = DSL.field(DSL.name("asset_tx_page", "block"), Long.class);
        Field<Long> blockTimeField = DSL.field(DSL.name("asset_tx_page", "block_time"), Long.class);

        var query = dsl.select(txHashField, txIndexField, blockField, blockTimeField).from(assetTxPage);
        logQuery("assetTxPagePostgres", query);
        return query.fetch(record -> new AssetTxRow(
                record.get(txHashField),
                record.get(txIndexField),
                record.get(blockField),
                record.get(blockTimeField)
        ));
    }

    /**
     * Fallback for non-Postgres dialects (H2/MySQL tests): join every UTXO-bearing tx and sort.
     * Correct but not slot-windowed, which is fine for the small test datasets.
     */
    private List<AssetTxRow> assetTxPageFallback(String unit, int page, int count, Order order) {
        int offset = Math.max(page, 0) * count;
        Table<?> distinctAssetTx = dsl.select(ADDRESS_UTXO.TX_HASH.as("tx_hash"))
                .from(ADDRESS_UTXO)
                .where(buildUnitCondition(unit))
                .groupBy(ADDRESS_UTXO.TX_HASH)
                .asTable("distinct_asset_tx");
        Field<String> distinctTxHashField = distinctAssetTx.field("tx_hash", String.class);

        Field<String> txHashField = TRANSACTION.TX_HASH;
        Field<Long> blockHeightField = TRANSACTION.BLOCK.as("block_height");
        Field<Long> blockTimeField = TRANSACTION.BLOCK_TIME.as("block_time");
        Field<Integer> txIndexSortField = TRANSACTION.TX_INDEX;
        Field<Long> txIndexField = DSL.coalesce(txIndexSortField, 0).cast(Long.class).as("tx_index");

        SortField<?> blockOrder = order == Order.desc
                ? TRANSACTION.BLOCK.desc().nullsLast()
                : TRANSACTION.BLOCK.asc().nullsLast();
        SortField<?> txIndexOrder = order == Order.desc
                ? txIndexSortField.desc().nullsLast()
                : txIndexSortField.asc().nullsFirst();
        SortField<?> txOrder = order == Order.desc ? txHashField.desc() : txHashField.asc();

        var query = dsl.select(txHashField, txIndexField, blockHeightField, blockTimeField)
                .from(distinctAssetTx)
                .join(TRANSACTION)
                .on(TRANSACTION.TX_HASH.eq(distinctTxHashField))
                .orderBy(blockOrder, txIndexOrder, txOrder)
                .limit(count)
                .offset(offset);

        logQuery("assetTxPageFallback", query);
        return query.fetch(record -> new AssetTxRow(
                record.get(txHashField),
                record.get(txIndexField),
                record.get(blockHeightField),
                record.get(blockTimeField)
        ));
    }

    private record AssetTxRow(String txHash, Long txIndex, Long block, Long blockTime) {
    }

    /**
     * Resolves the first mint row per unit for the requested page/order.
     * Strategy:
     * 1) build first mint slot per unit (distinct on unit),
     * 2) compute page boundary slots from a small ordered window,
     * 3) enrich only candidate rows inside boundary range,
     * 4) rank in final order and slice requested page.
     */
    private List<FirstSeenUnit> findFirstSeenUnitsPage(int offset, int count, Order order) {
        if (BlockfrostDialectUtil.isPostgres(dsl)) {
            return findFirstSeenUnitsPagePostgres(offset, count, order);
        }
        return findFirstSeenUnitsPageNonPostgres(offset, count, order);
    }

    private List<FirstSeenUnit> findFirstSeenUnitsPagePostgres(int offset, int count, Order order) {
        String slotOrder = order == Order.desc ? "desc nulls last" : "asc nulls last";
        String txIndexOrder = order == Order.desc ? "desc" : "asc";
        String unitOrder = order == Order.desc ? "desc" : "asc";

        // Boundary comparisons for slot range selection
        // For ASC: we want the page_window starting from offset, boundary is min/max of that window
        // For DESC: reversed
        String boundaryStartAgg = order == Order.desc ? "max" : "min";
        String boundaryEndAgg = order == Order.desc ? "min" : "max";

        // prefix counts rows strictly before the boundary slot
        String prefixCmp = order == Order.desc ? ">" : "<";
        // candidates are rows within the boundary slot range (inclusive)
        String candCmp1 = order == Order.desc ? "<=" : ">=";
        String candCmp2 = order == Order.desc ? ">=" : "<=";

        Table<?> firstSeenUnits = DSL.table(
                """
                (
                    with first_mints as materialized (
                        select distinct on (unit) unit as unit, slot::bigint as slot
                        from assets
                        where mint_type = 'MINT'
                        order by unit, slot asc nulls last
                    ),
                    page_window as materialized (
                        select unit, slot
                        from first_mints
                        order by slot %s, unit %s
                        offset {0} rows fetch next {1} rows only
                    ),
                    boundary as (
                        select %s(slot) as start_slot, %s(slot) as end_slot
                        from page_window
                    ),
                    prefix as (
                        select count(*) as cnt
                        from first_mints
                        where slot %s (select start_slot from boundary)
                    ),
                    candidates as (
                        select fm.unit, fm.slot
                        from first_mints fm, boundary b
                        where fm.slot %s b.start_slot
                          and fm.slot %s b.end_slot
                    ),
                    enriched as (
                        select c.unit, c.slot, picked.tx_hash, picked.tx_index
                        from candidates c
                        join lateral (
                            select a.tx_hash, t.tx_index
                            from assets a
                            join transaction t on t.tx_hash = a.tx_hash
                            where a.mint_type = 'MINT'
                              and a.unit = c.unit
                              and a.slot = c.slot
                            order by t.tx_index asc, a.tx_hash asc
                            fetch next 1 rows only
                        ) picked on true
                    ),
                    ranked as (
                        select unit, slot, tx_hash,
                               row_number() over (order by slot %s, tx_index %s, unit %s) as rn
                        from enriched
                    )
                    select unit, slot, tx_hash
                    from ranked
                    where rn > ({0} - (select cnt from prefix))
                      and rn <= ({0} - (select cnt from prefix) + {1})
                ) as first_seen_units
                """.formatted(
                        slotOrder, unitOrder,
                        boundaryStartAgg, boundaryEndAgg,
                        prefixCmp,
                        candCmp1, candCmp2,
                        slotOrder, txIndexOrder, unitOrder
                ),
                DSL.inline(offset), DSL.inline(count)
        );

        Field<String> unitField = DSL.field(DSL.name("first_seen_units", "unit"), String.class);
        Field<Long> slotField = DSL.field(DSL.name("first_seen_units", "slot"), Long.class);
        Field<String> txHashField = DSL.field(DSL.name("first_seen_units", "tx_hash"), String.class);

        var query = dsl.select(unitField, slotField, txHashField)
                .from(firstSeenUnits);
        logQuery("findAssetsUnitsPage", query);

        return query.fetch(record -> new FirstSeenUnit(
                record.get(unitField),
                record.get(slotField),
                record.get(txHashField)
        ));
    }

    private List<FirstSeenUnit> findFirstSeenUnitsPageNonPostgres(int offset, int count, Order order) {
        Field<String> unitField = ASSETS.UNIT.as("unit");
        Field<Long> slotField = DSL.min(ASSETS.SLOT).as("slot");
        Field<String> txHashField = DSL.min(ASSETS.TX_HASH).as("tx_hash");
        SortField<?> slotOrder = order == Order.desc ? slotField.desc().nullsLast() : slotField.asc().nullsLast();
        SortField<?> txHashOrder = order == Order.desc ? txHashField.desc() : txHashField.asc();
        SortField<?> unitOrder = order == Order.desc ? unitField.desc() : unitField.asc();

        var query = dsl.select(unitField, slotField, txHashField)
                .from(ASSETS)
                .where(ASSETS.MINT_TYPE.eq("MINT"))
                .groupBy(ASSETS.UNIT)
                .orderBy(slotOrder, txHashOrder, unitOrder)
                .limit(count)
                .offset(offset);

        logQuery("findAssetsUnitsPageNonPostgres", query);

        return query.fetch(record -> new FirstSeenUnit(
                record.get(unitField),
                record.get(slotField),
                record.get(txHashField)
        ));
    }

    /**
     * Builds JSONB containment condition for unit matching inside address_utxo.amounts.
     */
    private Condition buildUnitCondition(String unit) {
        if (BlockfrostDialectUtil.isPostgres(dsl)) {
            return DSL.condition("amounts @> {0}::jsonb", DSL.val("[{\"unit\": \"" + unit + "\"}]"));
        }

        return DSL.field(ADDRESS_UTXO.AMOUNTS).cast(String.class).contains("\"unit\": \"" + unit + "\"")
                .or(DSL.field(ADDRESS_UTXO.AMOUNTS).cast(String.class).contains("\"unit\":\"" + unit + "\""));
    }

    /**
     * Builds asset filter condition.
     * For valid unit strings (policy + asset name), it also adds policy equality for better index usage.
     */
    private Condition buildAssetCondition(String unit) {
        if (unit == null || unit.length() < 56) {
            return ASSETS.UNIT.eq(unit);
        }

        return ASSETS.UNIT.eq(unit).and(ASSETS.POLICY.eq(extractPolicy(unit)));
    }

    /**
     * Extracts policy id from a full unit (first 56 hex chars).
     */
    private String extractPolicy(String unit) {
        if (unit == null || unit.length() < 56) {
            return null;
        }
        return unit.substring(0, 56);
    }

    private record FirstSeenUnit(String unit, Long slot, String txHash) {
    }

    private record HolderAggregate(BigInteger quantity, Long firstSeenSlot) {
        private HolderAggregate merge(BigInteger additionalQuantity, Long slot) {
            BigInteger mergedQuantity = quantity.add(additionalQuantity);
            Long mergedFirstSeenSlot;
            if (firstSeenSlot == null) {
                mergedFirstSeenSlot = slot;
            } else if (slot == null) {
                mergedFirstSeenSlot = firstSeenSlot;
            } else {
                mergedFirstSeenSlot = Math.min(firstSeenSlot, slot);
            }
            return new HolderAggregate(mergedQuantity, mergedFirstSeenSlot);
        }
    }

    private Comparator<Long> slotComparator(Order order) {
        return (left, right) -> {
            if (left == null && right == null) {
                return 0;
            }
            if (left == null) {
                return 1;
            }
            if (right == null) {
                return -1;
            }
            return order == Order.desc ? right.compareTo(left) : left.compareTo(right);
        };
    }

    /**
     * Normalizes numeric results from jOOQ records into BigInteger.
     */
    private BigInteger toBigInteger(Object value) {
        if (value == null) {
            return BigInteger.ZERO;
        }
        if (value instanceof BigInteger bigInteger) {
            return bigInteger;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal.toBigInteger();
        }
        if (value instanceof Number number) {
            return BigInteger.valueOf(number.longValue());
        }
        return new BigInteger(String.valueOf(value));
    }

    /**
     * Emits full SQL with inlined parameters for debugging and plan analysis.
     */
    private void logQuery(String queryName, Query query) {
        log.debug("[BFAssetStorageReader] {} SQL: {}", queryName, query.getSQL(ParamType.INLINED));
    }
}
