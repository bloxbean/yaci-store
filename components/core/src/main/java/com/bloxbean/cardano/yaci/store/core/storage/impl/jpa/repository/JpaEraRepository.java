package com.bloxbean.cardano.yaci.store.core.storage.impl.jpa.repository;

import com.bloxbean.cardano.yaci.store.core.storage.impl.jpa.model.JpaEraEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface JpaEraRepository extends JpaRepository<JpaEraEntity, Integer> {

    @Query("select e from JpaEraEntity e where e.era > 1 order by e.era asc limit 1")
    Optional<JpaEraEntity> findFirstNonByronEra();
}
