package com.bloxbean.cardano.yaci.store.extensions.redis.utxo.impl;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.TxInput;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.events.internal.CommitEvent;
import com.bloxbean.cardano.yaci.store.extensions.redis.utxo.impl.mapper.RedisTxInputMapper;
import com.bloxbean.cardano.yaci.store.extensions.redis.utxo.impl.mapper.RedisUtxoMapper;
import com.bloxbean.cardano.yaci.store.extensions.redis.utxo.impl.repository.RedisTxInputRepository;
import com.bloxbean.cardano.yaci.store.extensions.redis.utxo.impl.repository.RedisUtxoRepository;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorage;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.UtxoCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@RequiredArgsConstructor
public class RedisUtxoStorage implements UtxoStorage {

    private final RedisUtxoRepository redisUtxoRepository;
    private final RedisTxInputRepository redisTxInputRepository;
    //    private final DSLContext dsl;
    private final RedisUtxoMapper mapper = RedisUtxoMapper.INSTANCE;
    private final RedisTxInputMapper redisTxInputMapper = RedisTxInputMapper.INSTANCE;
    private final UtxoCache utxoCache;

    @Override
    public Optional<AddressUtxo> findById(String txHash, int outputIndex) {
        var cacheUtxo = utxoCache.get(txHash, outputIndex);
        if (cacheUtxo.isPresent())
            return cacheUtxo;
        else {
            var savedUtxo = redisUtxoRepository.findById(txHash + "#" + outputIndex)
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

        List<String> redisUtxoIds = notFoundKeys.stream()
                .map(utxoKey -> utxoKey.getTxHash() + "#" + utxoKey.getOutputIndex())
                .toList();

        var savedUtxos = redisUtxoRepository.findAllById(redisUtxoIds)
                .stream().map(mapper::toAddressUtxo)
                .toList();

        //Add remaining utxos to cache
        savedUtxos.forEach(utxoCache::add);

        List<AddressUtxo> finalUtxos = new ArrayList<>();
        finalUtxos.addAll(cacheResult._1);
        finalUtxos.addAll(savedUtxos);

        return finalUtxos;
    }

    @Override
    public int deleteUnspentBySlotGreaterThan(Long slot) {
        Integer count = redisUtxoRepository.deleteBySlotGreaterThan(slot);
        return count == null ? 0 : count;
    }

    @Override
    public int deleteSpentBySlotGreaterThan(Long slot) {
        Integer count = redisTxInputRepository.deleteBySpentAtSlotGreaterThan(slot);
        return count == null ? 0 : count;
    }

    @Override
    public int deleteBySpentAndBlockLessThan(Long block) {
        AtomicReference<Integer> cnt = new AtomicReference<>(0);
        redisTxInputRepository.findByTxHashIsNotNullAndSpentAtBlockLessThan(block)
                .forEach(redisTxInputEntity -> {
                    Integer count = redisUtxoRepository.deleteByTxHashAndOutputIndex(redisTxInputEntity.getTxHash(), redisTxInputEntity.getOutputIndex());
                    cnt.updateAndGet(v -> v + count);
                });
        return cnt.get();
    }

    @Override
    public void saveUnspent(List<AddressUtxo> addressUtxoList) {
        redisUtxoRepository.saveAll(addressUtxoList.stream()
                .map(mapper::toAddressUtxoEntity)
                .toList());
        addressUtxoList.forEach(utxoCache::add);
    }

    @Override
    public void saveSpent(List<TxInput> txInputs) {
        if (txInputs == null || txInputs.isEmpty())
            return;
        redisTxInputRepository.saveAll(txInputs.stream().map(redisTxInputMapper::toRedisTxInputEntity).toList());
    }

    @EventListener
    public void handleCommit(CommitEvent commitEvent) {
        utxoCache.clear();
    }
}
