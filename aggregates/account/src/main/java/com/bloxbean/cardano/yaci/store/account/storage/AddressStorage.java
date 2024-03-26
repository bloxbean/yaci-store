package com.bloxbean.cardano.yaci.store.account.storage;

import com.bloxbean.cardano.yaci.store.account.domain.Address;

import java.util.Collection;

public interface AddressStorage {
    void save(Collection<Address> addresses);
}
