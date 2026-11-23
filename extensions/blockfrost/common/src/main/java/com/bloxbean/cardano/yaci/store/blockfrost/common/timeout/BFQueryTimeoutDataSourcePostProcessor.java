package com.bloxbean.cardano.yaci.store.blockfrost.common.timeout;

import org.springframework.beans.factory.config.BeanPostProcessor;

import javax.sql.DataSource;

/**
 * Wraps the application's main {@code dataSource} bean in {@link BFQueryTimeoutDataSource}. Other data sources, such
 * as the analytics DuckDB writer, are left as they are.
 */
public class BFQueryTimeoutDataSourcePostProcessor implements BeanPostProcessor {

    static final String DATA_SOURCE_BEAN = "dataSource";

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (DATA_SOURCE_BEAN.equals(beanName) && bean instanceof DataSource dataSource
                && !(bean instanceof BFQueryTimeoutDataSource))
            return new BFQueryTimeoutDataSource(dataSource);
        return bean;
    }
}
