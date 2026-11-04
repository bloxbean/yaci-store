package com.bloxbean.cardano.yaci.store.blockfrost.common.timeout;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.hibernate.engine.spi.SessionImplementor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.UndeclaredThrowableException;

/**
 * Bounds Spring Data repository calls made while serving a Blockfrost request, e.g. the shared
 * {@code TransactionStorageReader} or {@code EpochStakeStorageReader} used by the Blockfrost services.
 * <p>
 * The call runs in a read-only transaction with the request's remaining budget set as Hibernate's deadline, which
 * gives each statement at least 1 s while time is left. A timeout on the Spring transaction is avoided on purpose:
 * Spring passes it as a query hint in milliseconds, which Hibernate rounds to 0 (no timeout) under 500 ms.
 * Without a budget (indexer threads) the call proceeds untouched.
 */
public class BFRepositoryTimeoutInterceptor implements MethodInterceptor {

    private final ObjectProvider<PlatformTransactionManager> transactionManager;
    private final ObjectProvider<EntityManagerFactory> entityManagerFactory;

    public BFRepositoryTimeoutInterceptor(ObjectProvider<PlatformTransactionManager> transactionManager,
                                          ObjectProvider<EntityManagerFactory> entityManagerFactory) {
        this.transactionManager = transactionManager;
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        if (!BFQueryTimeoutContext.isActive())
            return invocation.proceed();

        PlatformTransactionManager txManager = transactionManager.getIfUnique();
        if (txManager == null)
            return invocation.proceed();

        TransactionTemplate template = new TransactionTemplate(txManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        template.setReadOnly(true);
        int seconds = BFQueryTimeoutContext.remainingSeconds();
        RuntimeException[] timeout = new RuntimeException[1];

        try {
            return template.execute(status -> {
                applyDeadline(seconds);
                try {
                    return invocation.proceed();
                } catch (RuntimeException e) {
                    if (BFQueryTimeouts.isTimeout(e))
                        timeout[0] = e;
                    throw e;
                } catch (Error e) {
                    throw e;
                } catch (Throwable t) {
                    throw new UndeclaredThrowableException(t);
                }
            });
        } catch (UndeclaredThrowableException e) {
            throw e.getUndeclaredThrowable();
        } catch (RuntimeException e) {
            // A failed rollback replaces the original exception: the pool closes the connection on an
            // SQLTimeoutException (H2, MySQL), so the timeout seen in the callback takes precedence
            RuntimeException failure = timeout[0] != null ? timeout[0] : e;
            if (!BFQueryTimeouts.isTimeout(failure))
                throw e;

            BFQueryTimeoutContext.markTimedOut();
            throw failure instanceof QueryTimeoutException ? failure
                    : new QueryTimeoutException("Blockfrost repository query timed out", failure);
        }
    }

    private void applyDeadline(int seconds) {
        EntityManagerFactory emf = entityManagerFactory.getIfUnique();
        EntityManager em = emf != null ? EntityManagerFactoryUtils.getTransactionalEntityManager(emf) : null;
        if (em != null)
            em.unwrap(SessionImplementor.class).getJdbcCoordinator().setTransactionTimeOut(seconds);
    }
}
