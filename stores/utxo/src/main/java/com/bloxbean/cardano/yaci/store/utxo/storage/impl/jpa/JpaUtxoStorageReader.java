package com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa;

import com.bloxbean.cardano.yaci.core.util.Tuple;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.TxInput;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorageReader;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.mapper.JpaUtxoMapper;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.model.JpaAddressUtxoEntity;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.model.JpaTxInputEntity;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.model.JpaUtxoId;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.repository.JpaTxInputRepository;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.repository.JpaUtxoRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.ADDRESS_UTXO;
import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.TX_INPUT;
import static org.jooq.impl.DSL.field;

@RequiredArgsConstructor
public class JpaUtxoStorageReader implements UtxoStorageReader {

    private final JpaUtxoRepository jpaUtxoRepository;
    private final JpaTxInputRepository spentOutputRepository;
    private final DSLContext dsl;
    private final JpaUtxoMapper mapper = JpaUtxoMapper.INSTANCE;

    @Override
    public Optional<AddressUtxo> findById(String txHash, int outputIndex) {
        return jpaUtxoRepository.findById(new JpaUtxoId(txHash, outputIndex))
                .map(mapper::toAddressUtxo);
    }

    @Override
    public List<AddressUtxo> findUtxoByAddress(@NonNull String address, int page, int count, Order order) {
        Pageable pageable = getPageable(page, count, order);

        return jpaUtxoRepository.findUnspentByOwnerAddr(address, pageable)
                .stream()
                .flatMap(addressUtxoEntities -> addressUtxoEntities.stream().map(mapper::toAddressUtxo))
                .toList();
    }

    @Override
    public List<AddressUtxo> findUtxosByAsset(String unit, int page, int count, Order order) {
        Pageable pageable = PageRequest.of(page, count)
                .withSort(order.equals(Order.desc) ? Sort.Direction.DESC : Sort.Direction.ASC, "slot", "txHash", "outputIndex");

        var query = dsl
                .select(ADDRESS_UTXO.fields())
                .from(ADDRESS_UTXO)
                .leftJoin(TX_INPUT)
                .using(field(ADDRESS_UTXO.TX_HASH), field(ADDRESS_UTXO.OUTPUT_INDEX))
                .where(field(ADDRESS_UTXO.AMOUNTS).cast(String.class).contains("\"unit\": \""+unit+"\""))
                .and(TX_INPUT.TX_HASH.isNull())
                //.orderBy(order.equals(Order.desc) ? ADDRESS_UTXO.SLOT.desc() : ADDRESS_UTXO.SLOT.asc())  //TODO: Ordering
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        return query.fetch().into(AddressUtxo.class);
    }

    @Override
    public List<AddressUtxo> findUtxoByAddressAndAsset(String address, String unit, int page, int count, Order order) {
        Pageable pageable = PageRequest.of(page, count)
                .withSort(order.equals(Order.desc) ? Sort.Direction.DESC : Sort.Direction.ASC, "slot", "txHash", "outputIndex");

        var query = dsl
                .select(ADDRESS_UTXO.fields())
                .from(ADDRESS_UTXO)
                .leftJoin(TX_INPUT)
                .using(field(ADDRESS_UTXO.TX_HASH), field(ADDRESS_UTXO.OUTPUT_INDEX))
                .where(ADDRESS_UTXO.OWNER_ADDR.eq(address))
                .and(TX_INPUT.TX_HASH.isNull())
                .and(field(ADDRESS_UTXO.AMOUNTS).cast(String.class).contains("\"unit\": \""+unit+"\""))
                //.orderBy(order.equals(Order.desc) ? ADDRESS_UTXO.SLOT.desc() : ADDRESS_UTXO.SLOT.asc())  //TODO: Ordering
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        return query.fetch().into(AddressUtxo.class);
    }

    @Override
    public List<AddressUtxo> findUtxoByPaymentCredential(@NonNull String paymentCredential, int page, int count, Order order) {
        Pageable pageable = PageRequest.of(page, count)
                .withSort(order.equals(Order.desc) ? Sort.Direction.DESC : Sort.Direction.ASC, "slot", "txHash", "outputIndex");

        return jpaUtxoRepository.findUnspentByOwnerPaymentCredential(paymentCredential, pageable)
                .stream()
                .flatMap(addressUtxoEntities -> addressUtxoEntities.stream().map(mapper::toAddressUtxo))
                .toList();
    }

    @Override
    public List<AddressUtxo> findUtxoByPaymentCredentialAndAsset(String paymentCredential, String unit, int page, int count, Order order) {
        Pageable pageable = PageRequest.of(page, count)
                .withSort(order.equals(Order.desc) ? Sort.Direction.DESC : Sort.Direction.ASC, "slot", "txHash", "outputIndex");

        var query = dsl
                .select(ADDRESS_UTXO.fields())
                .from(ADDRESS_UTXO)
                .leftJoin(TX_INPUT)
                .using(field(ADDRESS_UTXO.TX_HASH), field(ADDRESS_UTXO.OUTPUT_INDEX))
                .where(ADDRESS_UTXO.OWNER_PAYMENT_CREDENTIAL.eq(paymentCredential))
                .and(TX_INPUT.TX_HASH.isNull())
                .and(field(ADDRESS_UTXO.AMOUNTS).cast(String.class).contains("\"unit\": \""+unit+"\""))
                //.orderBy(order.equals(Order.desc) ? ADDRESS_UTXO.SLOT.desc() : ADDRESS_UTXO.SLOT.asc()) //TODO ordering
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        return query.fetch().into(AddressUtxo.class);
    }

    @Override
    public List<AddressUtxo> findUtxoByStakeAddress(@NonNull String stakeAddress, int page, int count, Order order) {
        Pageable pageable = PageRequest.of(page, count)
                .withSort(order.equals(Order.desc) ? Sort.Direction.DESC : Sort.Direction.ASC, "slot", "txHash", "outputIndex");

        return jpaUtxoRepository.findUnspentByOwnerStakeAddr(stakeAddress, pageable)
                .stream()
                .flatMap(addressUtxoEntities -> addressUtxoEntities.stream().map(mapper::toAddressUtxo))
                .toList();
    }

    @Override
    public List<AddressUtxo> findUtxoByStakeAddressAndAsset(@NonNull String stakeAddress, String unit, int page, int count, Order order) {
        stakeAddress = stakeAddress.trim();

        Pageable pageable = PageRequest.of(page, count)
                .withSort(order.equals(Order.desc) ? Sort.Direction.DESC : Sort.Direction.ASC, "slot", "txHash", "outputIndex");

        var query = dsl
                .select(ADDRESS_UTXO.fields())
                .from(ADDRESS_UTXO)
                .leftJoin(TX_INPUT)
                .using(field(ADDRESS_UTXO.TX_HASH), field(ADDRESS_UTXO.OUTPUT_INDEX))
                .where(ADDRESS_UTXO.OWNER_STAKE_ADDR.eq(stakeAddress))
                .and(TX_INPUT.TX_HASH.isNull())
                .and(field(ADDRESS_UTXO.AMOUNTS).cast(String.class).contains("\"unit\": \""+unit+"\""))
                // .orderBy(order.equals(Order.desc) ? ADDRESS_UTXO.SLOT.desc() : ADDRESS_UTXO.SLOT.asc())  //TODO: ordering
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        return query.fetch().into(AddressUtxo.class);
    }

    @Override
    public List<Long> findNextAvailableBlocks(Long block, int limit) {
        return jpaUtxoRepository.findNextAvailableBlocks(block, limit);
    }

    @Override
    public List<AddressUtxo> findAllByIds(List<UtxoKey> utxoKeys) {
        List<JpaUtxoId> jpaUtxoIds = utxoKeys.stream()
                .map(utxoKey -> new JpaUtxoId(utxoKey.getTxHash(), utxoKey.getOutputIndex()))
                .toList();

        return jpaUtxoRepository.findAllById(jpaUtxoIds)
                .stream().map(mapper::toAddressUtxo)
                .toList();
    }

    @Override
    public List<AddressUtxo> findUnspentUtxosBetweenBlocks(Long startBlock, Long endBlock) {
        return jpaUtxoRepository.findByBlockNumberBetween(startBlock, endBlock)
                .stream().map(mapper::toAddressUtxo)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<Tuple<AddressUtxo, TxInput>> findSpentUtxosBetweenBlocks(Long startBlock, Long endBlock) {
        List<Object[]> objects = jpaUtxoRepository.findBySpentAtBlockBetween(startBlock, endBlock);
        if (objects == null)
            return Collections.emptyList();

        return objects.stream().map(result -> {
            var addressUtxoEntity = (JpaAddressUtxoEntity) result[0];
            var addressUtxo = mapper.toAddressUtxo(addressUtxoEntity);

            var txInputEntity = (JpaTxInputEntity) result[1];
            var txInput = mapper.toTxInput(txInputEntity);

            return new Tuple<>(addressUtxo, txInput);
        }).collect(Collectors.toList());

    }

    private static PageRequest getPageable(int page, int count, Order order) {
        return PageRequest.of(page, count)
                .withSort(order.equals(Order.desc) ? Sort.Direction.DESC : Sort.Direction.ASC, "slot", "txHash", "outputIndex");
    }
}
