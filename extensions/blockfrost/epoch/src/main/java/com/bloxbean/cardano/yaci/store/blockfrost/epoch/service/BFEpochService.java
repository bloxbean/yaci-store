package com.bloxbean.cardano.yaci.store.blockfrost.epoch.service;

import com.bloxbean.cardano.yaci.store.adapot.domain.EpochStake;
import com.bloxbean.cardano.yaci.store.adapot.storage.EpochStakeStorageReader;
import com.bloxbean.cardano.yaci.store.api.epochaggr.service.EpochReadService;
import com.bloxbean.cardano.yaci.store.blockfrost.epoch.dto.BFEpochDto;
import com.bloxbean.cardano.yaci.store.blockfrost.epoch.dto.BFEpochStakeDto;
import com.bloxbean.cardano.yaci.store.blockfrost.epoch.dto.BFEpochStakePoolDto;
import com.bloxbean.cardano.yaci.store.blockfrost.epoch.mapper.BFEpochMapper;
import com.bloxbean.cardano.yaci.store.blockfrost.epoch.mapper.BFEpochStakeMapper;
import com.bloxbean.cardano.yaci.store.blockfrost.epoch.storage.BFEpochStorageReader;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.common.util.PoolUtil;
import com.bloxbean.cardano.yaci.store.epochaggr.domain.Epoch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BFEpochService {

    private final EpochReadService epochReadService;
    private final BFEpochStorageReader epochStorageReader;
    private final ObjectProvider<EpochStakeStorageReader> epochStakeStorageProvider;
    private final BFEpochMapper bfEpochMapper = BFEpochMapper.INSTANCE;
    private final BFEpochStakeMapper bfEpochStakeMapper = BFEpochStakeMapper.INSTANCE;

    public BFEpochDto getLatestEpoch() {
        Epoch epoch = epochReadService.getLatestEpoch()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Epoch not found"));

        return toBFEpochDto(epoch);
    }

    public BFEpochDto getEpoch(int number) {
        Epoch epoch = requireEpoch(number);
        return toBFEpochDto(epoch);
    }

    public List<BFEpochDto> getNextEpochs(int number, int page, int count) {
        requireEpoch(number);

        List<Epoch> epochs = epochStorageReader.findNextEpochs(number, page, count);
        Map<Integer, String> activeStakeMap = resolveActiveStakeMap(epochs);

        return epochs.stream()
                .map(epoch -> toBFEpochDto(epoch, activeStakeMap.get((int) epoch.getNumber())))
                .collect(Collectors.toList());
    }

    public List<BFEpochDto> getPreviousEpochs(int number, int page, int count) {
        requireEpoch(number);

        List<Epoch> epochs = epochStorageReader.findPreviousEpochs(number, page, count);
        Map<Integer, String> activeStakeMap = resolveActiveStakeMap(epochs);

        return epochs.stream()
                .map(epoch -> toBFEpochDto(epoch, activeStakeMap.get((int) epoch.getNumber())))
                .collect(Collectors.toList());
    }

    public List<BFEpochStakeDto> getEpochStakes(int number, int page, int count) {
        requireEpoch(number);

        EpochStakeStorageReader stakeStorageReader = epochStakeStorageProvider.getIfAvailable();
        if (stakeStorageReader == null) {
            return Collections.emptyList();
        }

        List<EpochStake> stakes = stakeStorageReader.getAllActiveStakesByEpoch(number, page, count);
        return stakes.stream()
                .map(bfEpochStakeMapper::toBFEpochStakeDto)
                .collect(Collectors.toList());
    }

    public List<BFEpochStakePoolDto> getEpochStakesByPool(int number, String poolId, int page, int count) {
        requireEpoch(number);

        EpochStakeStorageReader stakeStorageReader = epochStakeStorageProvider.getIfAvailable();
        if (stakeStorageReader == null) {
            return Collections.emptyList();
        }

        String poolHash = normalizePoolHash(poolId);
        List<EpochStake> stakes = stakeStorageReader.getAllActiveStakesByEpochAndPool(number, poolHash, page, count);
        return stakes.stream()
                .map(bfEpochStakeMapper::toBFEpochStakePoolDto)
                .collect(Collectors.toList());
    }

    public List<String> getEpochBlocks(int number, int page, int count, Order order) {
        requireEpoch(number);
        return epochStorageReader.findBlockHashesByEpoch(number, page, count, order);
    }

    public List<String> getEpochBlocksByPool(int number, String poolId, int page, int count, Order order) {
        requireEpoch(number);
        String poolHash = normalizePoolHash(poolId);
        return epochStorageReader.findBlockHashesByEpochAndPool(number, poolHash, page, count, order);
    }

    private Epoch requireEpoch(int number) {
        return epochReadService.getEpochByNumber(number)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Epoch not found: " + number));
    }

    private BFEpochDto toBFEpochDto(Epoch epoch) {
        String activeStake = resolveActiveStake(epoch.getNumber());
        return bfEpochMapper.toBFEpochDto(epoch, activeStake);
    }

    private BFEpochDto toBFEpochDto(Epoch epoch, String activeStake) {
        return bfEpochMapper.toBFEpochDto(epoch, activeStake);
    }

    private String resolveActiveStake(long epochNumber) {
        EpochStakeStorageReader stakeStorageReader = epochStakeStorageProvider.getIfAvailable();
        if (stakeStorageReader == null) {
            return null;
        }

        Optional<BigInteger> activeStake = stakeStorageReader.getTotalActiveStakeByEpoch((int) epochNumber);
        return activeStake.map(BigInteger::toString).orElse(null);
    }

    private Map<Integer, String> resolveActiveStakeMap(List<Epoch> epochs) {
        EpochStakeStorageReader stakeStorageReader = epochStakeStorageProvider.getIfAvailable();
        if (stakeStorageReader == null) {
            return Collections.emptyMap();
        }

        Map<Integer, BigInteger> activeStakeMap = epochStorageReader.getActiveStakesByEpochs(getEpochNumbers(epochs));
        return activeStakeMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().toString()));
    }

    private List<Integer> getEpochNumbers(List<Epoch> epochs) {
        return epochs.stream()
                .map(epoch -> (int) epoch.getNumber())
                .collect(Collectors.toList());
    }

    private String normalizePoolHash(String poolId) {
        if (poolId == null || poolId.isBlank()) {
            return poolId;
        }
        if (poolId.startsWith(PoolUtil.POOL_ID_PREFIX)) {
            try {
                return PoolUtil.getPoolHash(poolId);
            } catch (Exception e) {
                return poolId;
            }
        }
        return poolId;
    }

}
