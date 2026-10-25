package com.bloxbean.cardano.yaci.store.blockfrost.common.timeout;

import org.aopalliance.aop.Advice;
import org.springframework.aop.Advisor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/**
 * Adds {@link BFRepositoryTimeoutInterceptor} to every Spring Data repository proxy, just before the
 * repository's own {@link TransactionInterceptor}, so the read-only transaction it opens carries the timeout.
 */
public class BFRepositoryTimeoutPostProcessor implements BeanPostProcessor {

    private final Advice interceptor;

    public BFRepositoryTimeoutPostProcessor(Advice interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        // Runs before the factory bean's afterPropertiesSet(), which applies the customizers
        if (bean instanceof RepositoryFactoryBeanSupport<?, ?, ?> factoryBean) {
            factoryBean.addRepositoryFactoryCustomizer(factory -> factory.addRepositoryProxyPostProcessor(
                    (proxyFactory, repositoryInformation) -> addBeforeTransactionInterceptor(proxyFactory, interceptor)));
        }
        return bean;
    }

    static void addBeforeTransactionInterceptor(ProxyFactory proxyFactory, Advice advice) {
        Advisor[] advisors = proxyFactory.getAdvisors();
        int position = advisors.length;
        for (int i = 0; i < advisors.length; i++) {
            if (advisors[i].getAdvice() instanceof TransactionInterceptor) {
                position = i;
                break;
            }
        }
        proxyFactory.addAdvice(position, advice);
    }
}
