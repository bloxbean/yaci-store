package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.service;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.Cip113Configuration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigInteger;

/**
 * CIP-113 registry node detection service.
 * <p>
 * Only depends on {@link Cip113Configuration} (not on repositories) — follows the
 * yaci-store convention that services use StorageReader interfaces, not repositories directly.
 * Query methods live in {@link com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage.Cip113StorageReaderImpl}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class Cip113RegistryService {

    private final Cip113Configuration cip113Configuration;

    /**
     * Check if a UTxO contains a CIP-113 registry node NFT.
     */
    public boolean containsRegistryNode(AddressUtxo utxo) {
        if (!cip113Configuration.isEnabled()) {
            return false;
        }

        return utxo.getAmounts().stream()
                .anyMatch(amt -> amt.getQuantity().equals(BigInteger.ONE)
                        && cip113Configuration.isMonitoredPolicyId(amt.getPolicyId()));
    }

}
