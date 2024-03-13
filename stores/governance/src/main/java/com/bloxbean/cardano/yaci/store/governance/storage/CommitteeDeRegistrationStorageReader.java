package com.bloxbean.cardano.yaci.store.governance.storage;

import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeDeRegistration;

import java.util.List;

public interface CommitteeDeRegistrationStorageReader {
    List<CommitteeDeRegistration> findAll(int page, int count, Order order);
}
