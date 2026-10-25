package com.bloxbean.cardano.yaci.store.blockfrost.common.timeout;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.SimpleTransactionStatus;

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

        ObjectProvider<PlatformTransactionManager> provider = mock(ObjectProvider.class);
        when(provider.getIfUnique()).thenReturn(txManager);
        interceptor = new BFRepositoryTimeoutInterceptor(provider);
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
    void withBudget_callRunsInReadOnlyTransactionWithRemainingTime() throws Throwable {
        BFQueryTimeoutContext.begin(Duration.ofSeconds(30));
        MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.proceed()).thenReturn("row");

        assertThat(interceptor.invoke(invocation)).isEqualTo("row");

        ArgumentCaptor<TransactionDefinition> definition = ArgumentCaptor.forClass(TransactionDefinition.class);
        verify(txManager).getTransaction(definition.capture());
        assertThat(definition.getValue().isReadOnly()).isTrue();
        assertThat(definition.getValue().getTimeout()).isBetween(29, 30);
        assertThat(definition.getValue().getPropagationBehavior())
                .isEqualTo(TransactionDefinition.PROPAGATION_REQUIRED);
    }

    @Test
    void timeoutFromRepository_isRethrownAndMarked() throws Throwable {
        BFQueryTimeoutContext.begin(Duration.ofSeconds(30));
        MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.proceed()).thenThrow(new QueryTimeoutException("canceling statement"));

        assertThatThrownBy(() -> interceptor.invoke(invocation)).isInstanceOf(QueryTimeoutException.class);
        assertThat(BFQueryTimeoutContext.isTimedOut()).isTrue();
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
