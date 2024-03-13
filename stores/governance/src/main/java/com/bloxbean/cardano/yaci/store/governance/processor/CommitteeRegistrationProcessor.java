package com.bloxbean.cardano.yaci.store.governance.processor;

import com.bloxbean.cardano.yaci.core.model.certs.AuthCommitteeHotCert;
import com.bloxbean.cardano.yaci.core.model.certs.CertificateType;
import com.bloxbean.cardano.yaci.core.model.certs.ResignCommitteeColdCert;
import com.bloxbean.cardano.yaci.store.events.CertificateEvent;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.events.domain.TxCertificates;
import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeDeRegistration;
import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeRegistration;
import com.bloxbean.cardano.yaci.store.governance.storage.CommitteeDeRegistrationStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.CommitteeRegistrationStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommitteeRegistrationProcessor {
    private final CommitteeDeRegistrationStorage committeeDeRegistrationStorage;
    private final CommitteeRegistrationStorage committeeRegistrationStorage;

    @Transactional
    @EventListener
    public void handleCommitteeRegistration(CertificateEvent certificateEvent) {
        EventMetadata eventMetadata = certificateEvent.getMetadata();

        List<CommitteeDeRegistration> committeeDeRegistrations = new ArrayList<>();
        List<CommitteeRegistration> committeeRegistrations = new ArrayList<>();

        for (TxCertificates txCertificates : certificateEvent.getTxCertificatesList()) {
            String txHash = txCertificates.getTxHash();

            int index = 0;
            for (var certificate : txCertificates.getCertificates()) {
                if (certificate.getType() == CertificateType.RESIGN_COMMITTEE_COLD_CERT) {
                    ResignCommitteeColdCert resignCommitteeColdCert = (ResignCommitteeColdCert) certificate;

                    CommitteeDeRegistration committeeDeRegistration = CommitteeDeRegistration.builder()
                            .txHash(txHash)
                            .slot(eventMetadata.getSlot())
                            .blockNumber(eventMetadata.getBlock())
                            .blockTime(eventMetadata.getBlockTime())
                            .coldKey(resignCommitteeColdCert.getCommitteeColdCredential().getHash())
                            .credType(resignCommitteeColdCert.getCommitteeColdCredential().getType())
                            .epoch(eventMetadata.getEpochNumber())
                            .certIndex(index)
                            .build();

                    if (resignCommitteeColdCert.getAnchor() != null) {
                        committeeDeRegistration.setAnchorHash(resignCommitteeColdCert.getAnchor().getAnchor_data_hash());
                        committeeDeRegistration.setAnchorUrl(resignCommitteeColdCert.getAnchor().getAnchor_url());
                    }

                    committeeDeRegistrations.add(committeeDeRegistration);
                } else if (certificate.getType() == CertificateType.AUTH_COMMITTEE_HOT_CERT) {
                    AuthCommitteeHotCert authCommitteeHotCert = (AuthCommitteeHotCert) certificate;

                    CommitteeRegistration committeeRegistration = CommitteeRegistration.builder()
                            .txHash(txHash)
                            .slot(eventMetadata.getSlot())
                            .blockNumber(eventMetadata.getBlock())
                            .blockTime(eventMetadata.getBlockTime())
                            .certIndex(index)
                            .coldKey(authCommitteeHotCert.getCommitteeColdCredential().getHash())
                            .hotKey(authCommitteeHotCert.getCommitteeHotCredential().getHash())
                            .credType(authCommitteeHotCert.getCommitteeColdCredential().getType())
                            .epoch(eventMetadata.getEpochNumber())
                            .build();

                    committeeRegistrations.add(committeeRegistration);
                }

                index++;
            }
        }

        if (!committeeRegistrations.isEmpty()) {
            committeeRegistrationStorage.saveAll(committeeRegistrations);
        }
        if (!committeeDeRegistrations.isEmpty()) {
            committeeDeRegistrationStorage.saveAll(committeeDeRegistrations);
        }
    }

    @EventListener
    @Transactional
    //TODO -- tests
    public void handleRollbackEvent(RollbackEvent rollbackEvent) {
        int count = committeeDeRegistrationStorage.deleteBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
        log.info("Rollback -- {} committee_de_registrations records", count);

        count = committeeRegistrationStorage.deleteBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
        log.info("Rollback -- {} committee_registrations records", count);
    }
}
