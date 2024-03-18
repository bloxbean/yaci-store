package com.bloxbean.cardano.yaci.store.core.storage.impl.redis;

import com.bloxbean.cardano.yaci.store.core.domain.CardanoEra;
import com.bloxbean.cardano.yaci.store.core.storage.EraStorage;
import com.bloxbean.cardano.yaci.store.core.storage.impl.redis.mapper.RedisEraMapper;
import com.bloxbean.cardano.yaci.store.core.storage.impl.redis.repository.RedisEraRepository;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class RedisEraStorage implements EraStorage {

    private final RedisEraRepository redisEraRepository;
    private final RedisEraMapper redisEraMapper;

    @Override
    public void saveEra(CardanoEra era) {
        redisEraRepository.findById(era.getEra().getValue())
                .ifPresentOrElse(eraEntity -> {
                    //TODO -- Do nothing
                }, () -> redisEraRepository.save(redisEraMapper.toEraEntity(era)));
    }

    @Override
    public Optional<CardanoEra> findEra(int era) {
        return redisEraRepository.findById(era)
                .map(redisEraMapper::toEra);
    }

    @Override
    public Optional<CardanoEra> findFirstNonByronEra() {
        return redisEraRepository.findFirstByEraGreaterThanOrderByEraAsc(1)
                .map(redisEraMapper::toEra);
    }
}
