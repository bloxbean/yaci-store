package com.bloxbean.cardano.yaci.store.protocolparams.storage.impl.jpa.repository;

import com.bloxbean.cardano.yaci.store.protocolparams.storage.impl.jpa.model.CostModelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CostModelRepository extends JpaRepository<CostModelEntity, String> {

}
