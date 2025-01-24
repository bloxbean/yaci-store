package com.bloxbean.cardano.yaci.store.governanceaggr.processor;

import com.bloxbean.cardano.yaci.core.model.certs.CertificateType;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.events.internal.PreCommitEvent;
import com.bloxbean.cardano.yaci.store.governance.domain.DRepRegistration;
import com.bloxbean.cardano.yaci.store.governance.domain.event.DRepRegistrationEvent;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.DRep;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.DRepStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.DRepStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
// todo: 'inactive', 'active again' case
public class DRepStatusProcessor {
    private final DRepStorage dRepStorage;

    private final List<DRepRegistrationEvent> dRepRegistrationsCache = Collections.synchronizedList(new ArrayList<>());

    @EventListener
    public void processDRepRegistrationEvent(DRepRegistrationEvent dRepRegistrationEvent) {
        dRepRegistrationsCache.add(dRepRegistrationEvent);
    }

    @EventListener
    @Transactional
    public void handleCommitEventToProcessDRepStatus(PreCommitEvent preCommitEvent) {
        try {
            dRepRegistrationsCache.sort(Comparator.comparingLong(e -> e.getMetadata().getSlot()));
            List<DRep> dReps = new ArrayList<>();

            for (var drepRegistrationEvent : dRepRegistrationsCache) {
                List<DRepRegistration> dRepRegistrations = drepRegistrationEvent.getDRepRegistrations();
                EventMetadata metadata = drepRegistrationEvent.getMetadata();
                dRepRegistrations.sort(Comparator.comparingLong(DRepRegistration::getTxIndex)
                        .thenComparingLong(DRepRegistration::getCertIndex));

                for (var drepRegistration : dRepRegistrations) {
                    var dRepBuilder = DRep.builder()
                            .drepId(drepRegistration.getDrepId())
                            .drepHash(drepRegistration.getDrepHash())
                            .txHash(drepRegistration.getTxHash())
                            .certIndex((int) drepRegistration.getCertIndex())
                            .txIndex(drepRegistration.getTxIndex())
                            .certType(drepRegistration.getType())
                            .deposit(drepRegistration.getDeposit())
                            .epoch(drepRegistration.getEpoch())
                            .slot(metadata.getSlot())
                            .blockHash(metadata.getBlockHash());

                    if (drepRegistration.getType() == CertificateType.REG_DREP_CERT) {
                        dRepBuilder.status(DRepStatus.ACTIVE);
                    } else {
                        var recentDRepOpt = dRepStorage.findRecentDRepRegistration(drepRegistration.getDrepId(), metadata.getEpochNumber());

                        if (recentDRepOpt.isEmpty()) {
                            var regisDRep = dReps.stream().filter(dRep -> dRep.getDrepId().equals(drepRegistration.getDrepId())).findFirst();

                            if (regisDRep.isEmpty()) {
                                log.error("Cannot find recent dRep registration");
                            } else {
                                dRepBuilder.deposit(regisDRep.get().getDeposit());
                            }
                        } else {
                            dRepBuilder.deposit(recentDRepOpt.get().getDeposit());
                        }

                        if (drepRegistration.getType() == CertificateType.UPDATE_DREP_CERT) {
                            dRepBuilder.status(DRepStatus.ACTIVE);
                        } else if (drepRegistration.getType() == CertificateType.UNREG_DREP_CERT) {
                            dRepBuilder.status(DRepStatus.RETIRED);
                        }
                    }
                    dReps.add(dRepBuilder.build());
                }
            }

            if (!dReps.isEmpty()) {
                dRepStorage.saveAll(dReps);
            }
        } finally {
            dRepRegistrationsCache.clear();
        }
    }

    @EventListener
    @Transactional
    public void handleRollbackEvent(RollbackEvent rollbackEvent) {
        int count = dRepStorage.deleteBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
        log.info("Rollback -- {} drep records", count);
    }
}
