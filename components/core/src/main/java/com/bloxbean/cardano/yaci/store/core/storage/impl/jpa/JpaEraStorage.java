package com.bloxbean.cardano.yaci.store.core.storage.impl.jpa;

import com.bloxbean.cardano.yaci.store.core.domain.CardanoEra;
import com.bloxbean.cardano.yaci.store.core.storage.EraStorage;
import com.bloxbean.cardano.yaci.store.core.storage.impl.jpa.mapper.JpaEraMapper;
import com.bloxbean.cardano.yaci.store.core.storage.impl.jpa.repository.JpaEraRepository;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class JpaEraStorage implements EraStorage {

    private final JpaEraRepository jpaEraRepository;
    private final JpaEraMapper jpaEraMapper;

    @Override
    public void saveEra(CardanoEra era) {
        jpaEraRepository.findById(era.getEra().getValue())
                .ifPresentOrElse(eraEntity -> {
                    //TODO -- Do nothing
                }, () -> jpaEraRepository.save(jpaEraMapper.toEraEntity(era)));
    }

    @Override
    public Optional<CardanoEra> findEra(int era) {
        return jpaEraRepository.findById(era)
                .map(jpaEraMapper::toEra);
    }

    @Override
    public Optional<CardanoEra> findFirstNonByronEra() {
        return jpaEraRepository.findFirstNonByronEra()
                .map(jpaEraMapper::toEra);
    }
}
