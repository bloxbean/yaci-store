package com.bloxbean.cardano.yaci.store.blockfrost.common.timeout;

import net.ttddyy.dsproxy.proxy.ProxyConfig;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The proxy exists only to bound statement time: nothing else of datasource-proxy may be switched on.
 */
class BFQueryTimeoutProxyConfigTest {

    private final JdbcDataSource h2 = new JdbcDataSource();
    private final BFQueryTimeoutDataSource dataSource;

    BFQueryTimeoutProxyConfigTest() {
        h2.setURL("jdbc:h2:mem:bf_proxy_config;DB_CLOSE_DELAY=-1");
        dataSource = new BFQueryTimeoutDataSource(h2);
    }

    @AfterEach
    void tearDown() {
        BFQueryTimeoutContext.clear();
    }

    @Test
    void onlyTheTimeoutListenerIsRegistered() {
        ProxyConfig config = dataSource.proxyConfig();

        assertThat(config.getQueryListener().getListeners())
                .singleElement().isInstanceOf(BFQueryTimeoutListener.class);
        assertThat(config.getMethodListener().getListeners()).isEmpty();
    }

    @Test
    void resultSetAndGeneratedKeysProxyingAreOff() {
        ProxyConfig config = dataSource.proxyConfig();

        assertThat(config.isResultSetProxyEnabled()).isFalse();
        assertThat(config.isGeneratedKeysProxyEnabled()).isFalse();
        assertThat(config.isAutoRetrieveGeneratedKeys()).isFalse();
        assertThat(config.isRetrieveIsolationLevel()).isFalse();
    }

    @Test
    void resultSetsAreTheDriversOwn() throws Exception {
        BFQueryTimeoutContext.begin(Duration.ofSeconds(30));

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("select 1");
             ResultSet resultSet = statement.executeQuery()) {
            assertThat(Proxy.isProxyClass(connection.getClass())).isTrue();
            assertThat(Proxy.isProxyClass(resultSet.getClass())).isFalse();
        }
    }
}
