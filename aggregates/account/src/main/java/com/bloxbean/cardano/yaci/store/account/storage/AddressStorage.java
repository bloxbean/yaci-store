package com.bloxbean.cardano.yaci.store.account.storage;

import com.bloxbean.cardano.yaci.store.account.domain.Address;

import java.util.Set;

public interface AddressStorage {
    void save(Set<Address> addresses);
}
