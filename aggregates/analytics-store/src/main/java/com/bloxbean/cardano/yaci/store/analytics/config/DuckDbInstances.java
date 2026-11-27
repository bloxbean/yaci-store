package com.bloxbean.cardano.yaci.store.analytics.config;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Opens the in-process DuckDB instances of the analytics store and the analytics query layer.
 *
 * <p>Each {@code jdbc:duckdb:} connection is a separate DuckDB instance. Several of them live in
 * one JVM: the export writer (one per physical pool connection), the short-lived instance of the
 * catalog pre-initializer, and the query layer's parent instance. Two problems follow if they are
 * opened with DuckDB's defaults:</p>
 * <ul>
 *   <li><b>Shared spill files.</b> The default {@code temp_directory} is {@code .tmp} in the working
 *       directory and every instance names its spill files the same way
 *       ({@code duckdb_temp_storage_DEFAULT-0.tmp}, ...). One instance overwrites the other's spill
 *       blocks, and an instance that closes deletes the other's files. Re-reading such a block
 *       crashes the JVM (SIGSEGV). Each instance therefore gets its own directory under
 *       {@link #tempDirectoryBase}: {@code <role>-<random id>}. The directory holds a locked
 *       {@value #LOCK_FILE} file while the instance is open; on close the instance's directory is
 *       removed, and directories left behind by a crashed process (lock no longer held) are removed
 *       the next time an instance is opened. Directories of other live instances, also those of
 *       other processes using the same export path, are never touched.</li>
 *   <li><b>Memory over-commit.</b> Without {@code memory_limit} every instance takes 80% of the RAM.
 *       See {@link #effectiveMemoryLimit}.</li>
 * </ul>
 */
@Slf4j
public final class DuckDbInstances {

    /** Lock file inside each instance's temp directory; held while the instance is open. */
    static final String LOCK_FILE = ".owner.lock";

    /** Share of the RAM not reserved for the JVM heap that the DuckDB instances may use in total. */
    static final double DEFAULT_MEMORY_SHARE = 0.5;
    static final long MIN_DEFAULT_MEMORY_BYTES = 256L * 1024 * 1024;

    /** A directory without a lock file younger than this may still be being created. */
    private static final Duration UNLOCKED_GRACE = Duration.ofMinutes(10);

    private DuckDbInstances() {
    }

    /** The kinds of DuckDB instance; the name prefixes the instance's temp directory. */
    public enum Role {
        WRITER("writer"),
        READER("reader"),
        CATALOG_INIT("catalog-init");

        private final String directoryPrefix;

        Role(String directoryPrefix) {
            this.directoryPrefix = directoryPrefix;
        }

        public String directoryPrefix() {
            return directoryPrefix;
        }
    }

    /** An open DuckDB instance together with its private temp directory. */
    public static final class Instance implements AutoCloseable {
        private final Connection connection;
        private final TempDirectory tempDirectory;

        private Instance(Connection connection, TempDirectory tempDirectory) {
            this.connection = connection;
            this.tempDirectory = tempDirectory;
        }

        /** The raw DuckDB connection (an {@code org.duckdb.DuckDBConnection}). */
        public Connection connection() {
            return connection;
        }

        public Path tempDirectory() {
            return tempDirectory.path();
        }

        /** Close the connection (DuckDB removes its spill files), then remove the directory. */
        @Override
        public void close() throws SQLException {
            try {
                connection.close();
            } finally {
                tempDirectory.release();
            }
        }
    }

    /**
     * Open a new DuckDB instance with its own temp directory, the effective memory limit,
     * {@code threads}, {@code max_temp_directory_size} and {@code extension_directories}.
     */
    public static Instance open(AnalyticsStoreProperties properties, Role role) throws SQLException {
        AnalyticsStoreProperties.DuckDb duckdb = properties.getDuckdb();
        Path base = tempDirectoryBase(properties);
        sweepStaleTempDirectories(base);
        TempDirectory tempDirectory = TempDirectory.allocate(base, role);

        Properties config = new Properties();
        config.setProperty("temp_directory", tempDirectory.path().toString());
        String memoryLimit = effectiveMemoryLimit(properties, role);
        if (memoryLimit != null) {
            config.setProperty("memory_limit", memoryLimit);
        }
        if (duckdb.getThreads() > 0) {
            config.setProperty("threads", Integer.toString(duckdb.getThreads()));
        }
        String maxTemp = duckdb.getMaxTempDirectorySize();
        if (maxTemp != null && !maxTemp.isBlank()) {
            config.setProperty("max_temp_directory_size", maxTemp.trim());
        }

        Connection connection = null;
        try {
            createFirstExtensionDirectory(duckdb.getExtensionDirectories());
            connection = DriverManager.getConnection("jdbc:duckdb:", config);
            String extensionDirectories = extensionDirectoriesSql(duckdb.getExtensionDirectories());
            if (extensionDirectories != null) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(extensionDirectories);
                }
            }
            log.debug("Opened DuckDB {} instance (temp_directory={}, memory_limit={})",
                    role.directoryPrefix(), tempDirectory.path(), memoryLimit != null ? memoryLimit : "DuckDB default");
            return new Instance(connection, tempDirectory);
        } catch (SQLException | RuntimeException e) {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ignored) {
                    // already failing
                }
            }
            tempDirectory.release();
            throw e;
        }
    }

    /**
     * A non-pooling {@link javax.sql.DataSource} that opens a new DuckDB instance per
     * {@code getConnection()}. Intended as the {@code dataSource} of a connection pool: closing a
     * returned connection closes the instance and removes its temp directory, so an evicted pool
     * connection never deletes the spill files of its replacement.
     */
    public static javax.sql.DataSource dataSource(AnalyticsStoreProperties properties, Role role) {
        return new InstanceDataSource(properties, role);
    }

    /**
     * Base directory for the per-instance temp directories:
     * {@code yaci.store.analytics.duckdb.temp-directory}, or {@code <export-path>/.tmp}.
     */
    public static Path tempDirectoryBase(AnalyticsStoreProperties properties) {
        String configured = properties.getDuckdb().getTempDirectory();
        Path base = configured != null && !configured.isBlank()
                ? Paths.get(configured.trim())
                : Paths.get(properties.getExportPath(), ".tmp");
        return base.toAbsolutePath().normalize();
    }

    /**
     * The {@code memory_limit} for one instance: the configured
     * {@code yaci.store.analytics.duckdb.memory-limit} (applies to each instance), or, when it is
     * not set, {@value #DEFAULT_MEMORY_SHARE} of the RAM left after the JVM's maximum heap, split
     * evenly between the long-lived DuckDB instances of this JVM (the export writer and, if
     * enabled, the query layer), at least 256 MB. DuckDB's own default (80% of the RAM for
     * <em>each</em> instance) over-commits as soon as both run next to a JVM heap.
     *
     * @return a DuckDB size string, or {@code null} to keep DuckDB's default (RAM size unknown)
     */
    public static String effectiveMemoryLimit(AnalyticsStoreProperties properties, Role role) {
        String configured = properties.getDuckdb().getMemoryLimit();
        if (configured != null && !configured.isBlank()) {
            return configured.trim();
        }
        long total = totalPhysicalMemory();
        int instances = longLivedInstanceCount(properties);
        String computed = defaultMemoryLimit(total, Runtime.getRuntime().maxMemory(), instances);
        if (computed == null) {
            log.warn("yaci.store.analytics.duckdb.memory-limit is not set and the RAM size is unknown; "
                    + "the DuckDB {} instance uses DuckDB's default (80% of the RAM). Set the property "
                    + "explicitly.", role.directoryPrefix());
        } else if (role != Role.CATALOG_INIT) {
            log.info("yaci.store.analytics.duckdb.memory-limit is not set; DuckDB {} instance uses "
                            + "memory_limit={} ({}% of the RAM not reserved for the JVM heap, shared by {} "
                            + "DuckDB instance(s)). Set the property to override (it applies to each instance).",
                    role.directoryPrefix(), computed, Math.round(DEFAULT_MEMORY_SHARE * 100), instances);
        }
        return computed;
    }

    /** Pure computation behind {@link #effectiveMemoryLimit}; {@code null} when RAM is unknown. */
    static String defaultMemoryLimit(long totalPhysicalBytes, long maxHeapBytes, int instances) {
        if (totalPhysicalBytes <= 0) {
            return null;
        }
        long heap = maxHeapBytes > 0 && maxHeapBytes < Long.MAX_VALUE ? maxHeapBytes : 0;
        long available = Math.max(0, totalPhysicalBytes - heap);
        long perInstance = (long) (available * DEFAULT_MEMORY_SHARE) / Math.max(1, instances);
        long mb = Math.max(MIN_DEFAULT_MEMORY_BYTES, perInstance) / (1024 * 1024);
        return mb + "MB";
    }

    /** Export writer (analytics enabled) + query layer (query enabled); at least 1. */
    static int longLivedInstanceCount(AnalyticsStoreProperties properties) {
        int count = (properties.isEnabled() ? 1 : 0) + (properties.getQuery().isEnabled() ? 1 : 0);
        return Math.max(1, count);
    }

    private static long totalPhysicalMemory() {
        try {
            if (ManagementFactory.getOperatingSystemMXBean()
                    instanceof com.sun.management.OperatingSystemMXBean os) {
                return os.getTotalMemorySize(); // container (cgroup) aware
            }
        } catch (RuntimeException | LinkageError e) {
            log.debug("Could not determine the RAM size: {}", e.toString());
        }
        return -1;
    }

    /**
     * {@code SET extension_directories = [...]} for the given directories, or {@code null} when
     * none are configured.
     */
    public static String extensionDirectoriesSql(List<String> directories) {
        if (directories == null) {
            return null;
        }
        String list = directories.stream()
                .filter(d -> d != null && !d.isBlank())
                .map(d -> "'" + Paths.get(d.trim()).toAbsolutePath().normalize().toString().replace("'", "''") + "'")
                .collect(java.util.stream.Collectors.joining(", "));
        return list.isEmpty() ? null : "SET extension_directories = [" + list + "];";
    }

    /** DuckDB installs into the first extension directory but does not create it. */
    static void createFirstExtensionDirectory(List<String> directories) throws SQLException {
        if (directories == null) {
            return;
        }
        for (String d : directories) {
            if (d != null && !d.isBlank()) {
                Path dir = Paths.get(d.trim());
                try {
                    Files.createDirectories(dir);
                } catch (IOException e) {
                    throw new SQLException("Cannot create the DuckDB extension directory " + dir.toAbsolutePath()
                            + " (yaci.store.analytics.duckdb.extension-directories): " + e, e);
                }
                return;
            }
        }
    }

    /**
     * Remove {@code <role>-*} directories under {@code base} whose lock file is not held by a live
     * DuckDB instance (left behind by a crashed or killed process). Directories that are locked,
     * by this or another process, are left alone.
     *
     * @return the number of directories removed
     */
    public static int sweepStaleTempDirectories(Path base) {
        if (!Files.isDirectory(base)) {
            return 0;
        }
        int removed = 0;
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(base, DuckDbInstances::isInstanceDirectory)) {
            for (Path dir : entries) {
                if (removeIfStale(dir)) {
                    removed++;
                }
            }
        } catch (IOException e) {
            log.debug("Could not scan DuckDB temp directory base {}: {}", base, e.toString());
        }
        if (removed > 0) {
            log.info("Removed {} stale DuckDB temp director{} under {}", removed, removed == 1 ? "y" : "ies", base);
        }
        return removed;
    }

    private static boolean isInstanceDirectory(Path path) {
        if (!Files.isDirectory(path)) {
            return false;
        }
        String name = path.getFileName().toString();
        for (Role role : Role.values()) {
            if (name.startsWith(role.directoryPrefix() + "-")) {
                return true;
            }
        }
        return false;
    }

    private static boolean removeIfStale(Path dir) {
        Path lockFile = dir.resolve(LOCK_FILE);
        if (!Files.exists(lockFile)) {
            // Possibly being created right now by another process: only remove old ones.
            try {
                FileTime modified = Files.getLastModifiedTime(dir);
                if (modified.toInstant().isAfter(Instant.now().minus(UNLOCKED_GRACE))) {
                    return false;
                }
            } catch (IOException e) {
                return false;
            }
            return deleteRecursively(dir);
        }
        try (FileChannel channel = FileChannel.open(lockFile, StandardOpenOption.WRITE)) {
            FileLock lock;
            try {
                lock = channel.tryLock();
            } catch (OverlappingFileLockException e) {
                return false; // held by a live instance of this JVM
            }
            if (lock == null) {
                return false; // held by a live instance of another process
            }
            try {
                return deleteRecursively(dir);
            } finally {
                if (lock.isValid()) {
                    lock.release();
                }
            }
        } catch (NoSuchFileException e) {
            return false; // released and removed concurrently
        } catch (IOException e) {
            log.debug("Could not check DuckDB temp directory {}: {}", dir, e.toString());
            return false;
        }
    }

    private static boolean deleteRecursively(Path dir) {
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(p);
            }
            return true;
        } catch (IOException e) {
            log.warn("Could not remove DuckDB temp directory {}: {}", dir, e.toString());
            return false;
        }
    }

    /** A private, locked temp directory of one DuckDB instance. */
    static final class TempDirectory {
        private final Path path;
        private final FileChannel channel;
        private final FileLock lock;
        private boolean released;

        private TempDirectory(Path path, FileChannel channel, FileLock lock) {
            this.path = path;
            this.channel = channel;
            this.lock = lock;
        }

        static TempDirectory allocate(Path base, Role role) throws SQLException {
            Path dir = base.resolve(role.directoryPrefix() + "-" + UUID.randomUUID().toString().substring(0, 8));
            FileChannel channel = null;
            try {
                Files.createDirectories(dir);
                channel = FileChannel.open(dir.resolve(LOCK_FILE),
                        StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                FileLock lock = channel.tryLock();
                if (lock == null) {
                    throw new IOException("lock file is held by another process");
                }
                return new TempDirectory(dir, channel, lock);
            } catch (IOException | OverlappingFileLockException e) {
                if (channel != null) {
                    try {
                        channel.close();
                    } catch (IOException ignored) {
                        // already failing
                    }
                }
                throw new SQLException("Cannot create the DuckDB temp directory " + dir
                        + " (yaci.store.analytics.duckdb.temp-directory, default <export-path>/.tmp): " + e, e);
            }
        }

        Path path() {
            return path;
        }

        synchronized void release() {
            if (released) {
                return;
            }
            released = true;
            try {
                if (lock.isValid()) {
                    lock.release();
                }
                channel.close();
            } catch (IOException e) {
                log.debug("Could not release the lock of {}: {}", path, e.toString());
            }
            deleteRecursively(path);
        }
    }

    private static final class InstanceDataSource implements javax.sql.DataSource {
        private final AnalyticsStoreProperties properties;
        private final Role role;
        private PrintWriter logWriter;
        private int loginTimeout;

        private InstanceDataSource(AnalyticsStoreProperties properties, Role role) {
            this.properties = properties;
            this.role = role;
        }

        @Override
        public Connection getConnection() throws SQLException {
            Instance instance = open(properties, role);
            return closingInstance(instance);
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return getConnection();
        }

        @Override
        public PrintWriter getLogWriter() {
            return logWriter;
        }

        @Override
        public void setLogWriter(PrintWriter out) {
            this.logWriter = out;
        }

        @Override
        public void setLoginTimeout(int seconds) {
            this.loginTimeout = seconds;
        }

        @Override
        public int getLoginTimeout() {
            return loginTimeout;
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException();
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            if (iface.isInstance(this)) {
                return iface.cast(this);
            }
            throw new SQLException("Not a wrapper for " + iface);
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return iface.isInstance(this);
        }
    }

    /** The instance's connection, whose {@code close()} also removes the temp directory. */
    private static Connection closingInstance(Instance instance) {
        Connection raw = instance.connection();
        InvocationHandler handler = (proxy, method, args) -> {
            if ("close".equals(method.getName()) && method.getParameterCount() == 0) {
                instance.close();
                return null;
            }
            try {
                return method.invoke(raw, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        };
        return (Connection) Proxy.newProxyInstance(
                DuckDbInstances.class.getClassLoader(), new Class<?>[]{Connection.class}, handler);
    }
}
