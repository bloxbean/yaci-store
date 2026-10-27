package com.bloxbean.cardano.yaci.store.blockfrost.common.timeout;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.UndeclaredThrowableException;

/**
 * Bounds Spring Data repository calls made while serving a Blockfrost request, e.g. the shared
 * {@code TransactionStorageReader} or {@code EpochStakeStorageReader} used by the Blockfrost services.
 * <p>
 * The call runs in a read-only transaction whose timeout is the request's remaining budget; Hibernate applies
 * the remaining transaction time to each statement. Without a budget (indexer threads) the call proceeds untouched.
 */
public class BFRepositoryTimeoutInterceptor implements MethodInterceptor {

    private final ObjectProvider<PlatformTransactionManager> transactionManager;

    public BFRepositoryTimeoutInterceptor(ObjectProvider<PlatformTransactionManager> transactionManager) {
        this.transactionManager = transactionManager;
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
        template.setTimeout(BFQueryTimeoutContext.remainingSeconds());

        try {
            return template.execute(status -> {
                try {
                    return invocation.proceed();
                } catch (RuntimeException | Error e) {
                    throw e;
                } catch (Throwable t) {
                    throw new UndeclaredThrowableException(t);
                }
            });
        } catch (UndeclaredThrowableException e) {
            throw e.getUndeclaredThrowable();
        } catch (RuntimeException e) {
            if (BFQueryTimeouts.isTimeout(e))
                BFQueryTimeoutContext.markTimedOut();
            throw e;
        }
    }
}
