package com.bloxbean.cardano.yaci.store.blockfrost.common.timeout;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Sits right above the driver and records every execution with the query timeout the driver received and the
 * physical connection it ran on. Timeouts are forwarded unless {@link #forwardTimeouts} is off, which lets a test
 * simulate a statement that finishes past its deadline without being cancelled.
 */
final class RecordingDataSource {

    record Executed(String sql, int timeoutSeconds, int connectionId) {
    }

    static final List<Executed> EXECUTED = new CopyOnWriteArrayList<>();
    static volatile boolean forwardTimeouts = true;

    private RecordingDataSource() {
    }

    static void reset() {
        EXECUTED.clear();
        forwardTimeouts = true;
    }

    static Executed last() {
        return EXECUTED.get(EXECUTED.size() - 1);
    }

    static DataSource wrap(DataSource target) {
        return (DataSource) Proxy.newProxyInstance(DataSource.class.getClassLoader(), new Class<?>[]{DataSource.class},
                (proxy, method, args) -> {
                    Object result = invoke(target, method, args);
                    return result instanceof Connection connection ? connection(connection) : result;
                });
    }

    private static Connection connection(Connection target) {
        int connectionId = System.identityHashCode(target);
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    Object result = invoke(target, method, args);
                    if (result instanceof Statement statement && method.getName().matches("createStatement|prepare.*"))
                        return statement(statement, args != null && args.length > 0 ? (String) args[0] : null,
                                connectionId);
                    return result;
                });
    }

    private static Statement statement(Statement target, String preparedSql, int connectionId) {
        int[] timeout = {0};
        Class<?> type = target instanceof java.sql.CallableStatement ? java.sql.CallableStatement.class
                : target instanceof java.sql.PreparedStatement ? java.sql.PreparedStatement.class : Statement.class;
        return (Statement) Proxy.newProxyInstance(Statement.class.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> {
                    if (method.getName().equals("setQueryTimeout")) {
                        timeout[0] = (int) args[0];
                        if (!forwardTimeouts)
                            return null;
                    }
                    if (method.getName().startsWith("execute")) {
                        String sql = preparedSql != null ? preparedSql
                                : args != null && args.length > 0 && args[0] instanceof String s ? s : "<batch>";
                        EXECUTED.add(new Executed(sql, timeout[0], connectionId));
                    }
                    return invoke(target, method, args);
                });
    }

    private static Object invoke(Object target, Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
