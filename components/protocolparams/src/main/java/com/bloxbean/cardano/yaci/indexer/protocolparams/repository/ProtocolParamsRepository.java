package com.bloxbean.cardano.yaci.indexer.protocolparams.repository;

import com.bloxbean.cardano.yaci.indexer.protocolparams.entity.ProtocolParamsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProtocolParamsRepository extends JpaRepository<ProtocolParamsEntity, Long> {
}
