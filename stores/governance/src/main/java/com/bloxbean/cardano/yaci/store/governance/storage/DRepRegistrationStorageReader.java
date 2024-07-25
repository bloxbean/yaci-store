package com.bloxbean.cardano.yaci.store.governance.storage;

import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.governance.domain.DRepRegistration;

import java.util.List;

public interface DRepRegistrationStorageReader {
    List<DRepRegistration> findRegistrations(int page, int count, Order order);

    List<DRepRegistration> findDeRegistrations(int page, int count, Order order);

    List<DRepRegistration> findUpdates(int page, int count, Order order);

    List<DRepRegistration> findAll(int page, int count, Order order);
}
