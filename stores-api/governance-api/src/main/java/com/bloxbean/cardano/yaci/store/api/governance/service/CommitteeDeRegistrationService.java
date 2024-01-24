package com.bloxbean.cardano.yaci.store.api.governance.service;

import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeDeRegistration;
import com.bloxbean.cardano.yaci.store.governance.storage.CommitteeDeRegistrationStorageReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommitteeDeRegistrationService {
    private final CommitteeDeRegistrationStorageReader committeeDeRegistrationStorageReader;

    public List<CommitteeDeRegistration> getCommitteeDeRegistrations(int page, int count, Order order) {
        return committeeDeRegistrationStorageReader.findAll(page, count, order);
    }
}
