package com.bloxbean.cardano.yaci.store.script.storage.impl.redis.repository;

import com.bloxbean.cardano.yaci.store.script.storage.impl.jpa.model.JpaDatumEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RedisDatumRepository extends JpaRepository<JpaDatumEntity, String> {
    Optional<JpaDatumEntity> findByHash(String hash);
}
