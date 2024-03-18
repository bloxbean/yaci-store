package com.bloxbean.cardano.yaci.store.script.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.script.storage.impl.model.DatumEntityJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DatumRepository extends JpaRepository<DatumEntityJpa, String> {
    Optional<DatumEntityJpa> findByHash(String hash);
}
