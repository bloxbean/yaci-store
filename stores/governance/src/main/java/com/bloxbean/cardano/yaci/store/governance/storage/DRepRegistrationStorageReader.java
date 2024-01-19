package com.bloxbean.cardano.yaci.store.governance.storage;

import com.bloxbean.cardano.yaci.store.governance.domain.DRepRegistration;

import java.util.List;

public interface DRepRegistrationStorageReader {
    List<DRepRegistration> findRegistrations(int page, int count);

    List<DRepRegistration> findDeRegistrations(int page, int count);

    List<DRepRegistration> findUpdates(int page, int count);
}
