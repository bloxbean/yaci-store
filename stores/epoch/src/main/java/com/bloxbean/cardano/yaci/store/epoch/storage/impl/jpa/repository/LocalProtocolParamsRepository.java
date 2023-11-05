package com.bloxbean.cardano.yaci.store.epoch.storage.impl.jpa.repository;

import com.bloxbean.cardano.yaci.store.epoch.storage.impl.jpa.model.LocalProtocolParamsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocalProtocolParamsRepository extends JpaRepository<LocalProtocolParamsEntity, Long> {
}
