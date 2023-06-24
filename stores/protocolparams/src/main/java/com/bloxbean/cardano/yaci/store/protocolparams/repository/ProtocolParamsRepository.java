package com.bloxbean.cardano.yaci.store.protocolparams.repository;

import com.bloxbean.cardano.yaci.store.protocolparams.model.ProtocolParamsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProtocolParamsRepository extends JpaRepository<ProtocolParamsEntity, Long> {
}
