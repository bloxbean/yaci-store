package com.bloxbean.cardano.yaci.store.api.governance.service;

import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.governance.domain.DRepRegistration;
import com.bloxbean.cardano.yaci.store.governance.storage.DRepRegistrationStorageReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DRepRegistrationService {
    private final DRepRegistrationStorageReader dRepRegistrationStorageReader;

    public List<DRepRegistration> getRegistrations(int page, int count, Order order) {
        return dRepRegistrationStorageReader.findRegistrations(page, count, order);
    }

    public List<DRepRegistration> getDeRegistrations(int page, int count, Order order) {
        return dRepRegistrationStorageReader.findDeRegistrations(page, count, order);
    }

    public List<DRepRegistration> getUpdates(int page, int count, Order order) {
        return dRepRegistrationStorageReader.findUpdates(page, count, order);
    }
}
