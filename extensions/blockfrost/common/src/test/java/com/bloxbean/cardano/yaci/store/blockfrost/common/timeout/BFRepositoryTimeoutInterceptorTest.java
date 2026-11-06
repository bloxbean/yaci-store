package com.bloxbean.cardano.yaci.store.blockfrost.common.timeout;

import jakarta.persistence.EntityManagerFactory;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.sql.SQLException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BFRepositoryTimeoutInterceptorTest {

    private PlatformTransactionManager txManager;
    private BFRepositoryTimeoutInterceptor interceptor;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        txManager = mock(PlatformTransactionManager.class);
        when(txManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());

        ObjectProvider<PlatformTransactionManager> transactionManagers = mock(ObjectProvider.class);
        when(transactionManagers.getIfUnique()).thenReturn(txManager);
        ObjectProvider<EntityManagerFactory> entityManagerFactories = mock(ObjectProvider.class);
        interceptor = new BFRepositoryTimeoutInterceptor(transactionManagers, entityManagerFactories);
    }

    @AfterEach
    void tearDown() {
        BFQueryTimeoutContext.clear();
    }

    @Test
    void withoutBudget_callProceedsWithoutTransaction() throws Throwable {
        MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.proceed()).thenReturn("row");

        assertThat(interceptor.invoke(invocation)).isEqualTo("row");
        verifyNoInteractions(txManager);
    }

    @Test
    void withBudget_callRunsInReadOnlyTransactionWithoutSpringTimeout() throws Throwable {
        BFQueryTimeoutContext.begin(Duration.ofSeconds(30));
        MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.proceed()).thenReturn("row");

        assertThat(interceptor.invoke(invocation)).isEqualTo("row");

        ArgumentCaptor<TransactionDefinition> definition = ArgumentCaptor.forClass(TransactionDefinition.class);
        verify(txManager).getTransaction(definition.capture());
        assertThat(definition.getValue().isReadOnly()).isTrue();
        assertThat(definition.getValue().getTimeout()).isEqualTo(TransactionDefinition.TIMEOUT_DEFAULT);
        assertThat(definition.getValue().getPropagationBehavior())
                .isEqualTo(TransactionDefinition.PROPAGATION_REQUIRED);
    }

    @Test
    void exhaustedBudget_failsBeforeOpeningATransaction() throws Throwable {
        BFQueryTimeoutContext.begin(Duration.ofMillis(1));
        Thread.sleep(5);

        assertThatThrownBy(() -> interceptor.invoke(mock(MethodInvocation.class)))
                .isInstanceOf(QueryTimeoutException.class);
        verifyNoInteractions(txManager);
    }

    @Test
    void queryTimeoutException_isRethrownAndMarked() throws Throwable {
        BFQueryTimeoutContext.begin(Duration.ofSeconds(30));
        MethodInvocation invocation = mock(MethodInvocation.class);
        QueryTimeoutException timeout = new QueryTimeoutException("canceling statement");
        when(invocation.proceed()).thenThrow(timeout);

        assertThatThrownBy(() -> interceptor.invoke(invocation)).isSameAs(timeout);
        assertThat(BFQueryTimeoutContext.isTimedOut()).isTrue();
    }

    @Test
    void cancelTranslatedAsResourceFailure_becomesQueryTimeoutException() throws Throwable {
        BFQueryTimeoutContext.begin(Duration.ofSeconds(30));
        MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.proceed()).thenThrow(new DataAccessResourceFailureException("jpa",
                new SQLException("ERROR: canceling statement due to user request", "57014")));

        assertThatThrownBy(() -> interceptor.invoke(invocation)).isInstanceOf(QueryTimeoutException.class);
        assertThat(BFQueryTimeoutContext.isTimedOut()).isTrue();
    }

    @Test
    void springTransactionDeadline_becomesQueryTimeoutException() throws Throwable {
        BFQueryTimeoutContext.begin(Duration.ofSeconds(30));
        MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.proceed()).thenThrow(new TransactionTimedOutException("Transaction timed out"));

        assertThatThrownBy(() -> interceptor.invoke(invocation)).isInstanceOf(QueryTimeoutException.class);
        assertThat(BFQueryTimeoutContext.isTimedOut()).isTrue();
    }

    @Test
    void otherFailure_isRethrownUnchangedAndNotMarked() throws Throwable {
        BFQueryTimeoutContext.begin(Duration.ofSeconds(30));
        MethodInvocation invocation = mock(MethodInvocation.class);
        DataAccessResourceFailureException failure = new DataAccessResourceFailureException("lock",
                new SQLException("out of shared memory", "53200"));
        when(invocation.proceed()).thenThrow(failure);

        assertThatThrownBy(() -> interceptor.invoke(invocation)).isSameAs(failure);
        assertThat(BFQueryTimeoutContext.isTimedOut()).isFalse();
    }

    @Test
    void checkedExceptionFromRepository_isRethrownUnwrapped() throws Throwable {
        BFQueryTimeoutContext.begin(Duration.ofSeconds(30));
        MethodInvocation invocation = mock(MethodInvocation.class);
        Exception checked = new Exception("checked");
        when(invocation.proceed()).thenThrow(checked);

        assertThatThrownBy(() -> interceptor.invoke(invocation)).isSameAs(checked);
    }

    @Test
    void interceptorIsPlacedBeforeTheRepositoryTransactionInterceptor() {
        ProxyFactory proxyFactory = new ProxyFactory();
        MethodInterceptor first = MethodInvocation::proceed;
        TransactionInterceptor transactionInterceptor = new TransactionInterceptor();
        proxyFactory.addAdvice(first);
        proxyFactory.addAdvice(transactionInterceptor);

        BFRepositoryTimeoutPostProcessor.addBeforeTransactionInterceptor(proxyFactory, interceptor);

        assertThat(proxyFactory.getAdvisors()).extracting(a -> a.getAdvice())
                .containsExactly(first, interceptor, transactionInterceptor);
    }
}
