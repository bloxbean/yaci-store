package com.bloxbean.cardano.yaci.store.api.governance.service;

import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeRegistration;
import com.bloxbean.cardano.yaci.store.governance.storage.CommitteeRegistrationStorageReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommitteeRegistrationService {
    private final CommitteeRegistrationStorageReader committeeRegistrationStorageReader;

    public List<CommitteeRegistration> getCommitteeRegistrations(int page, int count, Order order) {
        return committeeRegistrationStorageReader.findAll(page, count, order);
    }
}
