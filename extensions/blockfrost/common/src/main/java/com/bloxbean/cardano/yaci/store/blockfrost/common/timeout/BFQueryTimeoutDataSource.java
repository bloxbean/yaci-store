package com.bloxbean.cardano.yaci.store.blockfrost.common.timeout;

import net.ttddyy.dsproxy.proxy.ProxyConfig;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Bounds the statements of a Blockfrost request by its query time budget.
 * <p>
 * Only a thread with a budget gets a proxied connection; every other thread (indexer, AdaPot, migrations) gets the
 * pool's connection as is. A proxied connection checks the budget before each execution, so it is safe to return to
 * the pool and be reused by a thread without one.
 * <p>
 * The proxy runs only {@link BFQueryTimeoutListener}: no SQL logging, query counting, tracing, or result-set and
 * generated-keys proxying.
 */
public class BFQueryTimeoutDataSource extends DelegatingDataSource {

    private final ProxyDataSource proxied;

    public BFQueryTimeoutDataSource(DataSource target) {
        super(target);
        this.proxied = ProxyDataSourceBuilder.create(target)
                .name("blockfrost-query-timeout")
                .listener(new BFQueryTimeoutListener())
                .build();
    }

    @Override
    public Connection getConnection() throws SQLException {
        return BFQueryTimeoutContext.isActive() ? proxied.getConnection() : obtainTargetDataSource().getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return BFQueryTimeoutContext.isActive()
                ? proxied.getConnection(username, password)
                : obtainTargetDataSource().getConnection(username, password);
    }

    ProxyConfig proxyConfig() {
        return proxied.getProxyConfig();
    }
}
