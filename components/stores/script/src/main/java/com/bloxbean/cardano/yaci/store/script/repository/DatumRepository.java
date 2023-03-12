package com.bloxbean.cardano.yaci.store.script.repository;

import com.bloxbean.cardano.yaci.store.script.model.DatumEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DatumRepository extends JpaRepository<DatumEntity, String> {
    Optional<DatumEntity> findByHash(String hash);
}
