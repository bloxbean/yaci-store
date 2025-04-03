package com.bloxbean.cardano.yaci.store.governance.processor;

import com.bloxbean.cardano.client.governance.GovId;
import com.bloxbean.cardano.yaci.core.model.Credential;
import com.bloxbean.cardano.yaci.core.model.certs.*;
import com.bloxbean.cardano.yaci.core.model.governance.Anchor;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yaci.store.common.aspect.EnableIf;
import com.bloxbean.cardano.yaci.store.events.CertificateEvent;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.events.domain.TxCertificates;
import com.bloxbean.cardano.yaci.store.governance.domain.DRepRegistration;
import com.bloxbean.cardano.yaci.store.governance.domain.event.DRepRegistrationEvent;
import com.bloxbean.cardano.yaci.store.governance.storage.DRepRegistrationStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static com.bloxbean.cardano.yaci.store.governance.GovernanceStoreConfiguration.STORE_GOVERNANCE_ENABLED;

@Component
@RequiredArgsConstructor
@EnableIf(STORE_GOVERNANCE_ENABLED)
@Slf4j
public class DRepRegistrationProcessor {
    private final DRepRegistrationStorage drepRegistrationStorage;
    private final ApplicationEventPublisher publisher;

    @Transactional
    @EventListener
    public void handleDRepRegistration(CertificateEvent certificateEvent) {
        EventMetadata eventMetadata = certificateEvent.getMetadata();

        List<DRepRegistration> dRepRegistrations = new ArrayList<>();
        for (TxCertificates txCertificates : certificateEvent.getTxCertificatesList()) {
            String txHash = txCertificates.getTxHash();
            int certIndex = 0;
            int txIndex = txCertificates.getTxIndex();

            for (var certificate : txCertificates.getCertificates()) {
                DRepRegistration drepRegistration = switch (certificate.getType()) {
                    case REG_DREP_CERT -> {
                        RegDrepCert regDrepCert = (RegDrepCert) certificate;
                        yield buildDRepRegistration(regDrepCert.getDrepCredential(), regDrepCert.getAnchor(),
                                regDrepCert.getCoin(), certificate.getType(),
                                txHash, txIndex, certIndex, eventMetadata);
                    }
                    case UNREG_DREP_CERT -> {
                        UnregDrepCert unregDrepCert = (UnregDrepCert) certificate;
                        yield buildDRepRegistration(unregDrepCert.getDrepCredential(), null, unregDrepCert.getCoin(),
                                certificate.getType(), txHash, txIndex, certIndex, eventMetadata);
                    }
                    case UPDATE_DREP_CERT -> {
                        UpdateDrepCert updateDrepCert = (UpdateDrepCert) certificate;
                        yield buildDRepRegistration(updateDrepCert.getDrepCredential(), updateDrepCert.getAnchor(), null,
                                certificate.getType(), txHash, txIndex, certIndex, eventMetadata);
                    }
                    default -> null;
                };

                if (drepRegistration != null) {
                    dRepRegistrations.add(drepRegistration);
                }
                certIndex++;
            }
        }
        if (!dRepRegistrations.isEmpty()) {
            drepRegistrationStorage.saveAll(dRepRegistrations);

            publisher.publishEvent(new DRepRegistrationEvent(eventMetadata, dRepRegistrations));
        }
    }

    private DRepRegistration buildDRepRegistration(Credential drepCredential, Anchor anchor, BigInteger deposit,
                                                   CertificateType certificateType, String txHash,
                                                   int txIndex, int certIndex, EventMetadata eventMetadata) {
        DRepRegistration drepRegistration = DRepRegistration.builder()
                .txHash(txHash)
                .certIndex(certIndex)
                .txIndex(txIndex)
                .type(certificateType)
                .slot(eventMetadata.getSlot())
                .blockNumber(eventMetadata.getBlock())
                .blockTime(eventMetadata.getBlockTime())
                .epoch(eventMetadata.getEpochNumber())
                .drepHash(drepCredential.getHash())
                .credType(drepCredential.getType())
                .deposit(deposit)
                .build();

        if (drepCredential.getHash() != null) {
            if (drepCredential.getType() == StakeCredType.ADDR_KEYHASH) {
                drepRegistration.setDrepId(GovId.drepFromKeyHash(HexUtil.decodeHexString(drepCredential.getHash())));
            } else if (drepCredential.getType() == StakeCredType.SCRIPTHASH) {
                drepRegistration.setDrepId(GovId.drepFromScriptHash(HexUtil.decodeHexString(drepCredential.getHash())));
            }
        }

        if (anchor != null) {
            drepRegistration.setAnchorHash(anchor.getAnchor_data_hash());
            drepRegistration.setAnchorUrl(anchor.getAnchor_url());
        }

        return drepRegistration;
    }

    @EventListener
    @Transactional
    //TODO -- tests
    public void handleRollbackEvent(RollbackEvent rollbackEvent) {
        int count = drepRegistrationStorage.deleteBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
        log.info("Rollback -- {} drep_registration records", count);
    }
}
