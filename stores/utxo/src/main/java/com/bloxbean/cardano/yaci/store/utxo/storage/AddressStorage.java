package com.bloxbean.cardano.yaci.store.utxo.storage;

import com.bloxbean.cardano.yaci.store.utxo.domain.Address;

import java.util.Collection;

public interface AddressStorage {
    void save(Collection<Address> addresses);
}
