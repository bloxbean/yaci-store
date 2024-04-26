package com.bloxbean.cardano.yaci.store.utxo.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.utxo.storage.impl.model.AddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AddressRepository extends JpaRepository<AddressEntity, String> {

}
