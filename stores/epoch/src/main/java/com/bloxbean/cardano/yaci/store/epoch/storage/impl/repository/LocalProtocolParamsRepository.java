package com.bloxbean.cardano.yaci.store.epoch.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.epoch.storage.impl.model.LocalProtocolParamsEntityJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocalProtocolParamsRepository extends JpaRepository<LocalProtocolParamsEntityJpa, Long> {
}
