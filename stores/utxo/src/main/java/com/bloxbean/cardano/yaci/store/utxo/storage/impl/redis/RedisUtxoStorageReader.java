package com.bloxbean.cardano.yaci.store.utxo.storage.impl.redis;

import com.bloxbean.cardano.yaci.core.util.Tuple;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.TxInput;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorageReader;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.redis.mapper.RedisUtxoMapper;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.redis.model.RedisAddressUtxoEntity;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.redis.repository.RedisTxInputRepository;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.redis.repository.RedisUtxoRepository;
import com.redis.om.spring.search.stream.EntityStream;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class RedisUtxoStorageReader implements UtxoStorageReader {

    private final RedisUtxoRepository redisUtxoRepository;
    private final RedisTxInputRepository redisTxInputRepository;
    private final EntityStream entityStream;
    private final RedisUtxoMapper mapper = RedisUtxoMapper.INSTANCE;

    @Override
    public Optional<AddressUtxo> findById(String txHash, int outputIndex) {
        return redisUtxoRepository.findById(txHash+"#"+outputIndex)
                .map(mapper::toAddressUtxo);
    }

    @Override
    public List<AddressUtxo> findUtxoByAddress(@NonNull String address, int page, int count, Order order) {
        Pageable pageable = getPageable(page, count, order);


//        return redisUtxoRepository.findUnspentByOwnerAddr(address, pageable)
//                .stream()
//                .flatMap(addressUtxoEntities -> addressUtxoEntities.stream().map(mapper::toAddressUtxo))
//                .toList();
        return null;
    }

    @Override
    public List<AddressUtxo> findUtxosByAsset(String unit, int page, int count, Order order) {
        Pageable pageable = PageRequest.of(page, count)
                .withSort(order.equals(Order.desc) ? Sort.Direction.DESC : Sort.Direction.ASC, "slot", "txHash", "outputIndex");

//        return entityStream.of(RedisAddressUtxoEntity.class)
//                .filter()

//        var query = dsl
//                .select(ADDRESS_UTXO.fields())
//                .from(ADDRESS_UTXO)
//                .leftJoin(TX_INPUT)
//                .using(field(ADDRESS_UTXO.TX_HASH), field(ADDRESS_UTXO.OUTPUT_INDEX))
//                .where(field(ADDRESS_UTXO.AMOUNTS).cast(String.class).contains("\"unit\": \""+unit+"\""))
//                .and(TX_INPUT.TX_HASH.isNull())
//                //.orderBy(order.equals(Order.desc) ? ADDRESS_UTXO.SLOT.desc() : ADDRESS_UTXO.SLOT.asc())  //TODO: Ordering
//                .offset(pageable.getOffset())
//                .limit(pageable.getPageSize());

//        return query.fetch().into(AddressUtxo.class);
        return null;
    }

    @Override
    public List<AddressUtxo> findUtxoByAddressAndAsset(String address, String unit, int page, int count, Order order) {
        Pageable pageable = PageRequest.of(page, count)
                .withSort(order.equals(Order.desc) ? Sort.Direction.DESC : Sort.Direction.ASC, "slot", "txHash", "outputIndex");

//        var query = dsl
//                .select(ADDRESS_UTXO.fields())
//                .from(ADDRESS_UTXO)
//                .leftJoin(TX_INPUT)
//                .using(field(ADDRESS_UTXO.TX_HASH), field(ADDRESS_UTXO.OUTPUT_INDEX))
//                .where(ADDRESS_UTXO.OWNER_ADDR.eq(address))
//                .and(TX_INPUT.TX_HASH.isNull())
//                .and(field(ADDRESS_UTXO.AMOUNTS).cast(String.class).contains("\"unit\": \""+unit+"\""))
//                //.orderBy(order.equals(Order.desc) ? ADDRESS_UTXO.SLOT.desc() : ADDRESS_UTXO.SLOT.asc())  //TODO: Ordering
//                .offset(pageable.getOffset())
//                .limit(pageable.getPageSize());
//
//        return query.fetch().into(AddressUtxo.class);
        return null;
    }

    @Override
    public List<AddressUtxo> findUtxoByPaymentCredential(@NonNull String paymentCredential, int page, int count, Order order) {
        Pageable pageable = PageRequest.of(page, count)
                .withSort(order.equals(Order.desc) ? Sort.Direction.DESC : Sort.Direction.ASC, "slot", "txHash", "outputIndex");

//        return redisUtxoRepository.findUnspentByOwnerPaymentCredential(paymentCredential, pageable)
//                .stream()
//                .flatMap(addressUtxoEntities -> addressUtxoEntities.stream().map(mapper::toAddressUtxo))
//                .toList();
        return null;
    }

    @Override
    public List<AddressUtxo> findUtxoByPaymentCredentialAndAsset(String paymentCredential, String unit, int page, int count, Order order) {
        Pageable pageable = PageRequest.of(page, count)
                .withSort(order.equals(Order.desc) ? Sort.Direction.DESC : Sort.Direction.ASC, "slot", "txHash", "outputIndex");

//        var query = dsl
//                .select(ADDRESS_UTXO.fields())
//                .from(ADDRESS_UTXO)
//                .leftJoin(TX_INPUT)
//                .using(field(ADDRESS_UTXO.TX_HASH), field(ADDRESS_UTXO.OUTPUT_INDEX))
//                .where(ADDRESS_UTXO.OWNER_PAYMENT_CREDENTIAL.eq(paymentCredential))
//                .and(TX_INPUT.TX_HASH.isNull())
//                .and(field(ADDRESS_UTXO.AMOUNTS).cast(String.class).contains("\"unit\": \""+unit+"\""))
//                //.orderBy(order.equals(Order.desc) ? ADDRESS_UTXO.SLOT.desc() : ADDRESS_UTXO.SLOT.asc()) //TODO ordering
//                .offset(pageable.getOffset())
//                .limit(pageable.getPageSize());
//
//        return query.fetch().into(AddressUtxo.class);

        return null;
    }

    @Override
    public List<AddressUtxo> findUtxoByStakeAddress(@NonNull String stakeAddress, int page, int count, Order order) {
        Pageable pageable = PageRequest.of(page, count)
                .withSort(order.equals(Order.desc) ? Sort.Direction.DESC : Sort.Direction.ASC, "slot", "txHash", "outputIndex");

//        return redisUtxoRepository.findUnspentByOwnerStakeAddr(stakeAddress, pageable)
//                .stream()
//                .flatMap(addressUtxoEntities -> addressUtxoEntities.stream().map(mapper::toAddressUtxo))
//                .toList();
        return null;
    }

    @Override
    public List<AddressUtxo> findUtxoByStakeAddressAndAsset(@NonNull String stakeAddress, String unit, int page, int count, Order order) {
        stakeAddress = stakeAddress.trim();

        Pageable pageable = PageRequest.of(page, count)
                .withSort(order.equals(Order.desc) ? Sort.Direction.DESC : Sort.Direction.ASC, "slot", "txHash", "outputIndex");

//        var query = dsl
//                .select(ADDRESS_UTXO.fields())
//                .from(ADDRESS_UTXO)
//                .leftJoin(TX_INPUT)
//                .using(field(ADDRESS_UTXO.TX_HASH), field(ADDRESS_UTXO.OUTPUT_INDEX))
//                .where(ADDRESS_UTXO.OWNER_STAKE_ADDR.eq(stakeAddress))
//                .and(TX_INPUT.TX_HASH.isNull())
//                .and(field(ADDRESS_UTXO.AMOUNTS).cast(String.class).contains("\"unit\": \""+unit+"\""))
//                // .orderBy(order.equals(Order.desc) ? ADDRESS_UTXO.SLOT.desc() : ADDRESS_UTXO.SLOT.asc())  //TODO: ordering
//                .offset(pageable.getOffset())
//                .limit(pageable.getPageSize());
//
//        return query.fetch().into(AddressUtxo.class);
        return null;
    }

    @Override
    public List<Long> findNextAvailableBlocks(Long block, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return redisUtxoRepository.findDistinctByBlockNumberGreaterThanEqualOrderByBlockNumberAsc(block, pageable).map(RedisAddressUtxoEntity::getBlockNumber).getContent();
    }

    @Override
    public List<AddressUtxo> findAllByIds(List<UtxoKey> utxoKeys) {
        List<String> redisUtxoIds = utxoKeys.stream()
                .map(utxoKey -> utxoKey.getTxHash()+"#"+utxoKey.getOutputIndex())
                .toList();

        return redisUtxoRepository.findAllById(redisUtxoIds)
                .stream().map(mapper::toAddressUtxo)
                .toList();
    }

    @Override
    public List<AddressUtxo> findUnspentUtxosBetweenBlocks(Long startBlock, Long endBlock) {
        return redisUtxoRepository.findByBlockNumberBetween(startBlock, endBlock)
                .stream().map(mapper::toAddressUtxo)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<Tuple<AddressUtxo, TxInput>> findSpentUtxosBetweenBlocks(Long startBlock, Long endBlock) {
        return null;
//        List<Object[]> objects = redisUtxoRepository.findBySpentAtBlockBetween(startBlock, endBlock);
//        if (objects == null)
//            return Collections.emptyList();
//
//        return objects.stream().map(result -> {
//            var addressUtxoEntity = (RedisAddressUtxoEntity) result[0];
//            var addressUtxo = mapper.toAddressUtxo(addressUtxoEntity);
//
//            var txInputEntity = (RedisTxInputEntity) result[1];
//            var txInput = mapper.toTxInput(txInputEntity);
//
//            return new Tuple<>(addressUtxo, txInput);
//        }).collect(Collectors.toList());

    }

    private static PageRequest getPageable(int page, int count, Order order) {
        return PageRequest.of(page, count)
                .withSort(order.equals(Order.desc) ? Sort.Direction.DESC : Sort.Direction.ASC, "slot", "txHash", "outputIndex");
    }
}
