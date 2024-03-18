package com.bloxbean.cardano.yaci.store.epoch.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.epoch.storage.impl.model.CostModelEntityJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CostModelRepository extends JpaRepository<CostModelEntityJpa, String> {

}
