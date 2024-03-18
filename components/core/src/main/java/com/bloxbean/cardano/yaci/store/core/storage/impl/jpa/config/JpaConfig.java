package com.bloxbean.cardano.yaci.store.core.storage.impl.jpa.config;

import com.bloxbean.cardano.yaci.store.core.storage.CursorStorage;
import com.bloxbean.cardano.yaci.store.core.storage.EraStorage;
import com.bloxbean.cardano.yaci.store.core.storage.config.StorageConfig;
import com.bloxbean.cardano.yaci.store.core.storage.impl.jpa.JpaCursorStorage;
import com.bloxbean.cardano.yaci.store.core.storage.impl.jpa.JpaEraStorage;
import com.bloxbean.cardano.yaci.store.core.storage.impl.jpa.mapper.JpaEraMapper;
import com.bloxbean.cardano.yaci.store.core.storage.impl.jpa.repository.JpaCursorRepository;
import com.bloxbean.cardano.yaci.store.core.storage.impl.jpa.repository.JpaEraRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class JpaConfig implements StorageConfig {

    private final JpaCursorRepository jpaCursorRepository;
    private final JpaEraRepository jpaEraRepository;
    private final JpaEraMapper jpaEraMapper;

    public CursorStorage cursorStorage() {
        return new JpaCursorStorage(jpaCursorRepository);
    }

    @Override
    public EraStorage eraStorage() {
        return new JpaEraStorage(jpaEraRepository, jpaEraMapper);
    }
}
