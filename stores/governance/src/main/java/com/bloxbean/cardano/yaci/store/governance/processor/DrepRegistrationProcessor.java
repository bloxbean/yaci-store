package com.bloxbean.cardano.yaci.store.governance.processor;

import com.bloxbean.cardano.yaci.core.model.Credential;
import com.bloxbean.cardano.yaci.core.model.certs.CertificateType;
import com.bloxbean.cardano.yaci.core.model.certs.RegDrepCert;
import com.bloxbean.cardano.yaci.core.model.certs.UnregDrepCert;
import com.bloxbean.cardano.yaci.core.model.certs.UpdateDrepCert;
import com.bloxbean.cardano.yaci.core.model.governance.Anchor;
import com.bloxbean.cardano.yaci.store.events.CertificateEvent;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.domain.TxCertificates;
import com.bloxbean.cardano.yaci.store.governance.domain.DrepRegistration;
import com.bloxbean.cardano.yaci.store.governance.storage.DrepRegistrationStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DrepRegistrationProcessor {
    private final DrepRegistrationStorage drepRegistrationStorage;

    @Transactional
    @EventListener
    public void handleDrepRegistration(CertificateEvent certificateEvent) {
        EventMetadata eventMetadata = certificateEvent.getMetadata();

        List<DrepRegistration> drepRegistrations = new ArrayList<>();
        for (TxCertificates txCertificates : certificateEvent.getTxCertificatesList()) {
            String txHash = txCertificates.getTxHash();
            int index = 0;

            for (var certificate : txCertificates.getCertificates()) {
                DrepRegistration drepRegistration = switch (certificate.getType()) {
                    case REG_DREP_CERT -> {
                        RegDrepCert regDrepCert = (RegDrepCert) certificate;
                        yield buildDrepRegistration(regDrepCert.getDrepCredential(), regDrepCert.getAnchor(),
                                regDrepCert.getCoin(), certificate.getType(),
                                txHash, index, eventMetadata);
                    }
                    case UNREG_DREP_CERT -> {
                        UnregDrepCert unregDrepCert = (UnregDrepCert) certificate;
                        yield buildDrepRegistration(unregDrepCert.getDrepCredential(), null, unregDrepCert.getCoin(),
                                certificate.getType(), txHash, index, eventMetadata);
                    }
                    case UPDATE_DREP_CERT -> {
                        UpdateDrepCert updateDrepCert = (UpdateDrepCert) certificate;
                        yield buildDrepRegistration(updateDrepCert.getDrepCredential(), updateDrepCert.getAnchor(), null,
                                certificate.getType(), txHash, index, eventMetadata);
                    }
                    default -> null;
                };

                if (drepRegistration != null) {
                    drepRegistrations.add(drepRegistration);
                }
                index++;
            }
        }
        if (!drepRegistrations.isEmpty()) {
            drepRegistrationStorage.saveAll(drepRegistrations);
        }
    }

    private DrepRegistration buildDrepRegistration(Credential drepCredential, Anchor anchor, BigInteger deposit,
                                                   CertificateType certificateType, String txHash,
                                                   int certIndex, EventMetadata eventMetadata) {
        DrepRegistration drepRegistration = DrepRegistration.builder()
                .txHash(txHash)
                .certIndex(certIndex)
                .type(certificateType)
                .slot(eventMetadata.getSlot())
                .blockNumber(eventMetadata.getBlock())
                .blockTime(eventMetadata.getBlockTime())
                .drepHash(drepCredential.getHash())
                .deposit(deposit)
                .build();
        // todo : drep view
        if (anchor != null) {
            drepRegistration.setAnchorHash(anchor.getAnchor_data_hash());
            drepRegistration.setAnchorUrl(anchor.getAnchor_url());
        }

        return drepRegistration;
    }
}
