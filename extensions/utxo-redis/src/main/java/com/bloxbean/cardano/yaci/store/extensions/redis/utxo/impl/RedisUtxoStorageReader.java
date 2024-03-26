package com.bloxbean.cardano.yaci.store.extensions.redis.utxo.impl;

import com.bloxbean.cardano.yaci.core.util.Tuple;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.TxInput;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.extensions.redis.utxo.impl.mapper.RedisUtxoMapper;
import com.bloxbean.cardano.yaci.store.extensions.redis.utxo.impl.model.RedisBlockAwareEntity;
import com.bloxbean.cardano.yaci.store.extensions.redis.utxo.impl.repository.RedisTxInputRepository;
import com.bloxbean.cardano.yaci.store.extensions.redis.utxo.impl.repository.RedisUtxoRepository;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorageReader;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class RedisUtxoStorageReader implements UtxoStorageReader {

    private final RedisUtxoRepository redisUtxoRepository;
    private final RedisTxInputRepository redisTxInputRepository;
    private final RedisUtxoMapper mapper = RedisUtxoMapper.INSTANCE;

    @Override
    public Optional<AddressUtxo> findById(String txHash, int outputIndex) {
        return redisUtxoRepository.findById(txHash + "#" + outputIndex)
                .map(mapper::toAddressUtxo);
    }

    @Override
    public List<AddressUtxo> findUtxoByAddress(@NonNull String address, int page, int count, Order order) {
        Pageable pageable = PageRequest.of(page, count)
                .withSort(order.equals(Order.desc) ? Sort.Direction.DESC : Sort.Direction.ASC, "slot", "txHash", "outputIndex");
        List<AddressUtxo> utxosByAddress = new ArrayList<>(redisUtxoRepository.findByOwnerAddr(address)
                .stream()
                .filter(redisAddressUtxoEntity -> StringUtils.isBlank(redisAddressUtxoEntity.getTxHash()))
                .map(mapper::toAddressUtxo)
                .toList());
        Comparator<AddressUtxo> comparator;
        if (order.equals(Order.asc)) {
            comparator = Comparator.comparing(AddressUtxo::getBlockTime);
        } else {
            comparator = Comparator.comparing(AddressUtxo::getBlockTime).reversed();
        }
        utxosByAddress.sort(comparator);
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), utxosByAddress.size());
        return utxosByAddress.subList(start, end);
    }

    @Override
    public List<AddressUtxo> findUtxosByAsset(@NonNull String unit, int page, int count, Order order) {
        Pageable pageable = PageRequest.of(page, count)
                .withSort(order.equals(Order.desc) ? Sort.Direction.DESC : Sort.Direction.ASC, "slot", "txHash", "outputIndex");

        List<AddressUtxo> utxosByAddress = new ArrayList<>(redisUtxoRepository.findByAmounts_Unit(unit)
                .stream()
                .filter(redisAddressUtxoEntity -> StringUtils.isBlank(redisAddressUtxoEntity.getTxHash()))
                .map(mapper::toAddressUtxo)
                .toList());
        Comparator<AddressUtxo> comparator;
        if (order.equals(Order.asc)) {
            comparator = Comparator.comparing(AddressUtxo::getBlockTime);
        } else {
            comparator = Comparator.comparing(AddressUtxo::getBlockTime).reversed();
        }
        utxosByAddress.sort(comparator);
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), utxosByAddress.size());
        return utxosByAddress.subList(start, end);
    }

    @Override
    public List<AddressUtxo> findUtxoByAddressAndAsset(@NonNull String address, @NonNull String unit, int page, int count, Order order) {
        Pageable pageable = PageRequest.of(page, count)
                .withSort(order.equals(Order.desc) ? Sort.Direction.DESC : Sort.Direction.ASC, "slot", "txHash", "outputIndex");
        List<AddressUtxo> utxosByAddress = new ArrayList<>(redisUtxoRepository.findByOwnerAddrAndAmounts_Unit(address, unit)
                .stream()
                .filter(redisAddressUtxoEntity -> StringUtils.isBlank(redisAddressUtxoEntity.getTxHash()))
                .map(mapper::toAddressUtxo)
                .toList());
        Comparator<AddressUtxo> comparator;
        if (order.equals(Order.asc)) {
            comparator = Comparator.comparing(AddressUtxo::getBlockTime);
        } else {
            comparator = Comparator.comparing(AddressUtxo::getBlockTime).reversed();
        }
        utxosByAddress.sort(comparator);
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), utxosByAddress.size());
        return utxosByAddress.subList(start, end);
    }

    @Override
    public List<AddressUtxo> findUtxoByPaymentCredential(@NonNull String paymentCredential, int page, int count, Order order) {
        Pageable pageable = PageRequest.of(page, count)
                .withSort(order.equals(Order.desc) ? Sort.Direction.DESC : Sort.Direction.ASC, "slot", "txHash", "outputIndex");
        List<AddressUtxo> utxosByAddress = new ArrayList<>(redisUtxoRepository.findByOwnerPaymentCredential(paymentCredential)
                .stream()
                .filter(redisAddressUtxoEntity -> StringUtils.isBlank(redisAddressUtxoEntity.getTxHash()))
                .map(mapper::toAddressUtxo)
                .toList());
        Comparator<AddressUtxo> comparator;
        if (order.equals(Order.asc)) {
            comparator = Comparator.comparing(AddressUtxo::getBlockTime);
        } else {
            comparator = Comparator.comparing(AddressUtxo::getBlockTime).reversed();
        }
        utxosByAddress.sort(comparator);
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), utxosByAddress.size());
        return utxosByAddress.subList(start, end);
    }

    @Override
    public List<AddressUtxo> findUtxoByPaymentCredentialAndAsset(@NonNull String paymentCredential, @NonNull String unit, int page, int count, Order order) {
        Pageable pageable = PageRequest.of(page, count)
                .withSort(order.equals(Order.desc) ? Sort.Direction.DESC : Sort.Direction.ASC, "slot", "txHash", "outputIndex");
        List<AddressUtxo> utxosByAddress = new ArrayList<>(redisUtxoRepository.findByOwnerPaymentCredentialAndAmounts_Unit(paymentCredential, unit)
                .stream()
                .filter(redisAddressUtxoEntity -> StringUtils.isBlank(redisAddressUtxoEntity.getTxHash()))
                .map(mapper::toAddressUtxo)
                .toList());
        Comparator<AddressUtxo> comparator;
        if (order.equals(Order.asc)) {
            comparator = Comparator.comparing(AddressUtxo::getBlockTime);
        } else {
            comparator = Comparator.comparing(AddressUtxo::getBlockTime).reversed();
        }
        utxosByAddress.sort(comparator);
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), utxosByAddress.size());
        return utxosByAddress.subList(start, end);
    }

    @Override
    public List<AddressUtxo> findUtxoByStakeAddress(@NonNull String stakeAddress, int page, int count, Order order) {
        Pageable pageable = PageRequest.of(page, count)
                .withSort(order.equals(Order.desc) ? Sort.Direction.DESC : Sort.Direction.ASC, "slot", "txHash", "outputIndex");
        List<AddressUtxo> utxosByAddress = new ArrayList<>(redisUtxoRepository.findByOwnerStakeAddr(stakeAddress)
                .stream()
                .filter(redisAddressUtxoEntity -> StringUtils.isBlank(redisAddressUtxoEntity.getTxHash()))
                .map(mapper::toAddressUtxo)
                .toList());
        Comparator<AddressUtxo> comparator;
        if (order.equals(Order.asc)) {
            comparator = Comparator.comparing(AddressUtxo::getBlockTime);
        } else {
            comparator = Comparator.comparing(AddressUtxo::getBlockTime).reversed();
        }
        utxosByAddress.sort(comparator);
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), utxosByAddress.size());
        return utxosByAddress.subList(start, end);
    }

    @Override
    public List<AddressUtxo> findUtxoByStakeAddressAndAsset(@NonNull String stakeAddress, @NonNull String unit, int page, int count, Order order) {
        Pageable pageable = PageRequest.of(page, count)
                .withSort(order.equals(Order.desc) ? Sort.Direction.DESC : Sort.Direction.ASC, "slot", "txHash", "outputIndex");
        List<AddressUtxo> utxosByAddress = new ArrayList<>(redisUtxoRepository.findByOwnerStakeAddrAndAmounts_Unit(stakeAddress, unit)
                .stream()
                .filter(redisAddressUtxoEntity -> StringUtils.isBlank(redisAddressUtxoEntity.getTxHash()))
                .map(mapper::toAddressUtxo)
                .toList());
        Comparator<AddressUtxo> comparator;
        if (order.equals(Order.asc)) {
            comparator = Comparator.comparing(AddressUtxo::getBlockTime);
        } else {
            comparator = Comparator.comparing(AddressUtxo::getBlockTime).reversed();
        }
        utxosByAddress.sort(comparator);
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), utxosByAddress.size());
        return utxosByAddress.subList(start, end);
    }

    @Override
    public List<Long> findNextAvailableBlocks(Long block, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return redisUtxoRepository.findDistinctByBlockNumberGreaterThanEqualOrderByBlockNumberAsc(block, pageable)
                .map(RedisBlockAwareEntity::getBlockNumber)
                .getContent();
    }

    @Override
    public List<AddressUtxo> findAllByIds(List<UtxoKey> utxoKeys) {
        List<String> redisUtxoIds = utxoKeys.stream()
                .map(utxoKey -> utxoKey.getTxHash() + "#" + utxoKey.getOutputIndex())
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

    @Override
    public List<Tuple<AddressUtxo, TxInput>> findSpentUtxosBetweenBlocks(Long startBlock, Long endBlock) {
        return redisTxInputRepository.findBySpentAtBlockBetween(startBlock, endBlock).stream()
                .map(redisTxInputEntity -> new Tuple<>(
                        redisUtxoRepository.findById(redisTxInputEntity.getTxHash() + "#" + redisTxInputEntity.getOutputIndex())
                                .map(mapper::toAddressUtxo).orElse(null),
                        mapper.toTxInput(redisTxInputEntity))).toList();
    }
}
