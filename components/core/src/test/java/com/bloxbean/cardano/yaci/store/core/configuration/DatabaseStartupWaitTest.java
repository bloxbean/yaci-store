package com.bloxbean.cardano.yaci.store.core.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.mockito.Mockito.*;

class DatabaseStartupWaitTest {

    @Test
    void waitsUntilTheDatabaseAnswers() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection())
                .thenThrow(new SQLException("the database system is starting up"))
                .thenReturn(mock(Connection.class));
        DatabaseStartupWait wait = new DatabaseStartupWait();
        wait.setEnvironment(new MockEnvironment().withProperty(DatabaseStartupWait.WAIT_SECONDS_PROPERTY, "30"));

        wait.postProcessAfterInitialization(dataSource, "dataSource");

        verify(dataSource, times(2)).getConnection();
    }

    @Test
    void zeroTurnsItOff() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        DatabaseStartupWait wait = new DatabaseStartupWait();
        wait.setEnvironment(new MockEnvironment().withProperty(DatabaseStartupWait.WAIT_SECONDS_PROPERTY, "0"));

        wait.postProcessAfterInitialization(dataSource, "dataSource");

        verify(dataSource, never()).getConnection();
    }

    @Test
    void otherBeansAreLeftAlone() {
        DatabaseStartupWait wait = new DatabaseStartupWait();
        Object bean = new Object();

        org.assertj.core.api.Assertions.assertThat(wait.postProcessAfterInitialization(bean, "x")).isSameAs(bean);
    }
}
