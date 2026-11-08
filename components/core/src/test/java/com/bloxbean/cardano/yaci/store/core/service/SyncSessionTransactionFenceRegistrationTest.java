package com.bloxbean.cardano.yaci.store.core.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizationAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring Boot adds TransactionExecutionListener beans to the auto-configured (JPA) transaction manager. The fence
 * relies on that. The application gets TransactionManagerCustomizationAutoConfiguration with the other
 * auto-configurations; the @DataJpaTest slice does not, so it is imported here.
 */
@DataJpaTest
@ImportAutoConfiguration(TransactionManagerCustomizationAutoConfiguration.class)
@Import({SyncSessionGuard.class, SyncSessionTransactionFence.class})
class SyncSessionTransactionFenceRegistrationTest {
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private SyncSessionTransactionFence fence;

    @Test
    void theFenceIsRegisteredWithTheTransactionManager() {
        assertThat(transactionManager).isInstanceOf(AbstractPlatformTransactionManager.class);
        assertThat(((AbstractPlatformTransactionManager) transactionManager).getTransactionExecutionListeners())
                .contains(fence);
    }
}
