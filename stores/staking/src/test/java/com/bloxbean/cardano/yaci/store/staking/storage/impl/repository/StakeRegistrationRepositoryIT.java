package com.bloxbean.cardano.yaci.store.staking.storage.impl.repository;

import com.bloxbean.cardano.yaci.core.model.certs.CertificateType;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.model.StakeRegistrationEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class StakeRegistrationRepositoryIT {

    @Autowired
    private StakeRegistrationRepository registrationRepository;

    @Test
    void findUnresolvedDeregistrations_ReturnsOnlyNullDepositsWithinSlotRange() {
        StakeRegistrationEntity unresolved = registration("unresolved", CertificateType.STAKE_DEREGISTRATION, null, 200);
        registrationRepository.saveAll(List.of(
                unresolved,
                registration("outside-range", CertificateType.STAKE_DEREGISTRATION, null, 500),
                registration("already-resolved", CertificateType.STAKE_DEREGISTRATION, BigInteger.TWO, 200),
                registration("registration", CertificateType.STAKE_REGISTRATION, null, 200)));

        List<StakeRegistrationEntity> result = registrationRepository.findUnresolvedDeregistrations(100, 300);

        assertThat(result).containsExactly(unresolved);
    }

    private StakeRegistrationEntity registration(String txHash,
                                                   CertificateType type,
                                                   BigInteger deposit,
                                                   long slot) {
        return StakeRegistrationEntity.builder()
                .txHash(txHash)
                .certIndex(0)
                .txIndex(0)
                .credential(txHash)
                .address("stake_test_" + txHash)
                .type(type)
                .deposit(deposit)
                .slot(slot)
                .build();
    }
}
