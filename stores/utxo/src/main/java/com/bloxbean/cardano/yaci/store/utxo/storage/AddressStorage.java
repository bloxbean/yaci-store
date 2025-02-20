package com.bloxbean.cardano.yaci.store.utxo.storage;

import com.bloxbean.cardano.yaci.store.utxo.domain.Address;
import com.bloxbean.cardano.yaci.store.utxo.domain.PtrAddress;

import java.util.Collection;
import java.util.List;

public interface AddressStorage {
    void save(Collection<Address> addresses);

    void savePtrAddress(Collection<PtrAddress> ptrAddresses);
    List<PtrAddress> findPtrAddresses();

    int deleteBySlotGreaterThan(long slot);
}
