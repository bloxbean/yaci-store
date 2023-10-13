package com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.common.util.JsonUtil;
import com.bloxbean.cardano.yaci.store.events.internal.CommitEvent;
import com.bloxbean.cardano.yaci.store.utxo.storage.api.UtxoStorage;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.mapper.UtxoMapper;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.model.AddressUtxoEntity;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.model.UtxoId;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.repository.UtxoRepository;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.JSON;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.ADDRESS_UTXO;
import static org.jooq.impl.DSL.field;

@RequiredArgsConstructor
@Slf4j
public class UtxoStorageImpl implements UtxoStorage {
    private final UtxoRepository utxoRepository;
    private final DSLContext dsl;
    private final UtxoMapper mapper = UtxoMapper.INSTANCE;

    private List<AddressUtxo> spentUtxoCache = new ArrayList<>();

    @Override
    public Optional<AddressUtxo> findById(String txHash, int outputIndex) {
        return utxoRepository.findById(new UtxoId(txHash, outputIndex))
                .map(entity -> mapper.toAddressUtxo(entity));
    }

    @Override
    public Optional<List<AddressUtxo>> findUtxoByAddress(String address, int page, int count, Order order) {
        return findUtxoByAddressAndSpent(address, null, page, count, order);
    }

    @Override
    public Optional<List<AddressUtxo>> findUtxoByAddressAndSpent(@NonNull String address, Boolean spent, int page, int count, Order order) {
        Pageable pageable = PageRequest.of(page, count)
                .withSort(order.equals(Order.desc) ? Sort.Direction.DESC : Sort.Direction.ASC, "slot");

        List<AddressUtxo> addressUtxoList = utxoRepository.findByOwnerAddrAndSpent(address, spent, pageable)
                .stream()
                .flatMap(addressUtxoEntities -> addressUtxoEntities.stream().map(mapper::toAddressUtxo))
                .toList();

        return Optional.of(addressUtxoList);
    }

    @Override
    public Optional<List<AddressUtxo>> findUtxoByAddressAndAsset(String address, String unit, int page, int count, Order order) {
        Pageable pageable = PageRequest.of(page, count)
                .withSort(order.equals(Order.desc) ? Sort.Direction.DESC : Sort.Direction.ASC, "slot");

        var query = dsl
                .select()
                .from(ADDRESS_UTXO)
                .where(ADDRESS_UTXO.OWNER_ADDR.eq(address))
                .and(ADDRESS_UTXO.SPENT.isNull())
                .and(field(ADDRESS_UTXO.AMOUNTS).cast(String.class).contains(unit))
                .orderBy(order.equals(Order.desc) ? ADDRESS_UTXO.SLOT.desc() : ADDRESS_UTXO.SLOT.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        List<AddressUtxo> addressUtxoList = query.fetch().into(AddressUtxo.class);
        return Optional.of(addressUtxoList);
    }

    @Override
    public Optional<List<AddressUtxo>> findUtxoByPaymentCredential(String paymentCredential, int page, int count, Order order) {
        return findUtxoByPaymentCredentialAndSpent(paymentCredential, null, page, count, order);
    }

    @Override
    public Optional<List<AddressUtxo>> findUtxoByPaymentCredentialAndSpent(@NonNull String paymentCredential, Boolean spent, int page, int count, Order order) {
        Pageable pageable = PageRequest.of(page, count)
                .withSort(order.equals(Order.desc) ? Sort.Direction.DESC : Sort.Direction.ASC, "slot");

        List<AddressUtxo> addressUtxoList = utxoRepository.findByOwnerPaymentCredentialAndSpent(paymentCredential, spent, pageable)
                .stream()
                .flatMap(addressUtxoEntities -> addressUtxoEntities.stream().map(mapper::toAddressUtxo))
                .toList();

        return Optional.of(addressUtxoList);
    }

    @Override
    public Optional<List<AddressUtxo>> findUtxoByPaymentCredentialAndAsset(String paymentCredential, String unit, int page, int count, Order order) {
        Pageable pageable = PageRequest.of(page, count)
                .withSort(order.equals(Order.desc) ? Sort.Direction.DESC : Sort.Direction.ASC, "slot");

        var query = dsl
                .select()
                .from(ADDRESS_UTXO)
                .where(ADDRESS_UTXO.OWNER_PAYMENT_CREDENTIAL.eq(paymentCredential))
                .and(ADDRESS_UTXO.SPENT.isNull())
                .and(field(ADDRESS_UTXO.AMOUNTS).cast(String.class).contains(unit))
                .orderBy(order.equals(Order.desc) ? ADDRESS_UTXO.SLOT.desc() : ADDRESS_UTXO.SLOT.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        List<AddressUtxo> addressUtxoList = query.fetch().into(AddressUtxo.class);
        return Optional.of(addressUtxoList);
    }

    @Override
    public Optional<List<AddressUtxo>> findUtxoByStakeAddress(@NonNull String stakeAddress, int page, int count, Order order) {
        Pageable pageable = PageRequest.of(page, count)
                .withSort(order.equals(Order.desc) ? Sort.Direction.DESC : Sort.Direction.ASC, "slot");

        List<AddressUtxo> addressUtxoList = utxoRepository.findByOwnerStakeAddrAndSpent(stakeAddress, null, pageable)
                .stream()
                .flatMap(addressUtxoEntities -> addressUtxoEntities.stream().map(mapper::toAddressUtxo))
                .toList();

        return Optional.of(addressUtxoList);
    }

    @Override
    public Optional<List<AddressUtxo>> findUtxoByStakeAddressAndAsset(@NonNull String stakeAddress, String unit, int page, int count, Order order) {
        stakeAddress = stakeAddress.trim();

        Pageable pageable = PageRequest.of(page, count)
                .withSort(order.equals(Order.desc) ? Sort.Direction.DESC : Sort.Direction.ASC, "slot");

        var query = dsl
                .select()
                .from(ADDRESS_UTXO)
                .where(ADDRESS_UTXO.OWNER_STAKE_ADDR.eq(stakeAddress))
                .and(ADDRESS_UTXO.SPENT.isNull())
                .and(field(ADDRESS_UTXO.AMOUNTS).cast(String.class).contains(unit))
                .orderBy(order.equals(Order.desc) ? ADDRESS_UTXO.SLOT.desc() : ADDRESS_UTXO.SLOT.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        List<AddressUtxo> addressUtxoList = query.fetch().into(AddressUtxo.class);
        return Optional.of(addressUtxoList);
    }

    @Override
    public List<AddressUtxo> findBySlot(Long slot) {
        return utxoRepository.findBySlot(slot)
                .stream().map(mapper::toAddressUtxo)
                .toList();
    }

    @Override
    public List<AddressUtxo> findAllByIds(List<UtxoKey> utxoKeys) {
        List<UtxoId> utxoIds = utxoKeys.stream()
                .map(utxoKey -> new UtxoId(utxoKey.getTxHash(), utxoKey.getOutputIndex()))
                .toList();

        return utxoRepository.findAllById(utxoIds)
                .stream().map(mapper::toAddressUtxo)
                .toList();
    }

    @Override
    public int deleteBySlotGreaterThan(Long slot) {
        return utxoRepository.deleteBySlotGreaterThan(slot);
    }

    @Override
    public void saveUnspent(List<AddressUtxo> addressUtxoList) {
        List<AddressUtxoEntity> addressUtxoEntities = addressUtxoList.stream()
                .map(addressUtxo -> mapper.toAddressUtxoEntity(addressUtxo))
                .toList();
//        addressUtxoEntities = utxoRepository.saveAll(addressUtxoEntities);
//        return Optional.of(addressUtxoEntities.stream()
//                .map(entity -> mapper.toAddressUtxo(entity))
//                .toList());

        LocalDateTime localDateTime = LocalDateTime.now();
        dsl.batched(c -> {
            for (AddressUtxoEntity addressUtxo : addressUtxoEntities) {
                c.dsl().insertInto(ADDRESS_UTXO)
                        .set(ADDRESS_UTXO.TX_HASH, addressUtxo.getTxHash())
                        .set(ADDRESS_UTXO.OUTPUT_INDEX, addressUtxo.getOutputIndex())
                        .set(ADDRESS_UTXO.SLOT, addressUtxo.getSlot())
                        .set(ADDRESS_UTXO.BLOCK_HASH, addressUtxo.getBlockHash())
                        .set(ADDRESS_UTXO.EPOCH, addressUtxo.getEpoch())
                        .set(ADDRESS_UTXO.LOVELACE_AMOUNT, addressUtxo.getLovelaceAmount() != null ? addressUtxo.getLovelaceAmount().longValue() : 0L)
                        .set(ADDRESS_UTXO.AMOUNTS, JSON.valueOf(JsonUtil.getJson(addressUtxo.getAmounts())))
                        .set(ADDRESS_UTXO.DATA_HASH, addressUtxo.getDataHash())
                        .set(ADDRESS_UTXO.INLINE_DATUM, addressUtxo.getInlineDatum())
                        .set(ADDRESS_UTXO.OWNER_ADDR, addressUtxo.getOwnerAddr())
                        .set(ADDRESS_UTXO.OWNER_ADDR_FULL, addressUtxo.getOwnerAddrFull())
                        .set(ADDRESS_UTXO.OWNER_STAKE_ADDR, addressUtxo.getOwnerStakeAddr())
                        .set(ADDRESS_UTXO.OWNER_PAYMENT_CREDENTIAL, addressUtxo.getOwnerPaymentCredential())
                        .set(ADDRESS_UTXO.OWNER_STAKE_CREDENTIAL, addressUtxo.getOwnerStakeCredential())
                        .set(ADDRESS_UTXO.SCRIPT_REF, addressUtxo.getScriptRef())
                        .set(ADDRESS_UTXO.REFERENCE_SCRIPT_HASH, addressUtxo.getReferenceScriptHash())
                        .set(ADDRESS_UTXO.IS_COLLATERAL_RETURN, addressUtxo.getIsCollateralReturn())
                        .set(ADDRESS_UTXO.BLOCK, addressUtxo.getBlockNumber())
                        .set(ADDRESS_UTXO.BLOCK_TIME, addressUtxo.getBlockTime())
                        .set(ADDRESS_UTXO.UPDATE_DATETIME, localDateTime)
                        .onDuplicateKeyUpdate()
                        .set(ADDRESS_UTXO.SLOT, addressUtxo.getSlot())
                        .set(ADDRESS_UTXO.BLOCK_HASH, addressUtxo.getBlockHash())
                        .set(ADDRESS_UTXO.EPOCH, addressUtxo.getEpoch())
                        .set(ADDRESS_UTXO.LOVELACE_AMOUNT, addressUtxo.getLovelaceAmount() != null ? addressUtxo.getLovelaceAmount().longValue() : 0L)
                        .set(ADDRESS_UTXO.AMOUNTS, JSON.valueOf(JsonUtil.getJson(addressUtxo.getAmounts())))
                        .set(ADDRESS_UTXO.DATA_HASH, addressUtxo.getDataHash())
                        .set(ADDRESS_UTXO.INLINE_DATUM, addressUtxo.getInlineDatum())
                        .set(ADDRESS_UTXO.OWNER_ADDR, addressUtxo.getOwnerAddr())
                        .set(ADDRESS_UTXO.OWNER_ADDR_FULL, addressUtxo.getOwnerAddrFull())
                        .set(ADDRESS_UTXO.OWNER_STAKE_ADDR, addressUtxo.getOwnerStakeAddr())
                        .set(ADDRESS_UTXO.OWNER_PAYMENT_CREDENTIAL, addressUtxo.getOwnerPaymentCredential())
                        .set(ADDRESS_UTXO.OWNER_STAKE_CREDENTIAL, addressUtxo.getOwnerStakeCredential())
                        .set(ADDRESS_UTXO.SCRIPT_REF, addressUtxo.getScriptRef())
                        .set(ADDRESS_UTXO.REFERENCE_SCRIPT_HASH, addressUtxo.getReferenceScriptHash())
                        .set(ADDRESS_UTXO.IS_COLLATERAL_RETURN, addressUtxo.getIsCollateralReturn())
                        .set(ADDRESS_UTXO.BLOCK, addressUtxo.getBlockNumber())
                        .set(ADDRESS_UTXO.BLOCK_TIME, addressUtxo.getBlockTime())
                        .set(ADDRESS_UTXO.UPDATE_DATETIME, localDateTime)
                        .execute();

            }
        });
    }

    @Override
    public void saveSpent(List<AddressUtxo> addressUtxoList) {
        if (addressUtxoList == null)
            return;

        spentUtxoCache.addAll(addressUtxoList);
    }

    @EventListener
    @Transactional
    public void handleCommit(CommitEvent event) {
        try {
            LocalDateTime localDateTime = LocalDateTime.now();
            dsl.batched(c -> {
                for (AddressUtxo addressUtxo : spentUtxoCache) {
                    c.dsl().insertInto(ADDRESS_UTXO)
                            .set(ADDRESS_UTXO.TX_HASH, addressUtxo.getTxHash())
                            .set(ADDRESS_UTXO.OUTPUT_INDEX, addressUtxo.getOutputIndex())
                            .set(ADDRESS_UTXO.SPENT, true)
                            .set(ADDRESS_UTXO.SPENT_AT_SLOT, addressUtxo.getSpentAtSlot())
                            .set(ADDRESS_UTXO.SPENT_EPOCH, addressUtxo.getSpentEpoch())
                            .set(ADDRESS_UTXO.SPENT_TX_HASH, addressUtxo.getSpentTxHash())
                            .set(ADDRESS_UTXO.UPDATE_DATETIME, localDateTime)
                            .onDuplicateKeyUpdate()
                            .set(ADDRESS_UTXO.SPENT, true)
                            .set(ADDRESS_UTXO.SPENT_AT_SLOT, addressUtxo.getSpentAtSlot())
                            .set(ADDRESS_UTXO.SPENT_EPOCH, addressUtxo.getSpentEpoch())
                            .set(ADDRESS_UTXO.SPENT_TX_HASH, addressUtxo.getSpentTxHash())
                            .set(ADDRESS_UTXO.UPDATE_DATETIME, localDateTime)
                            .execute();
                }
            });

        } finally {
            spentUtxoCache.clear();
        }
    }
}
