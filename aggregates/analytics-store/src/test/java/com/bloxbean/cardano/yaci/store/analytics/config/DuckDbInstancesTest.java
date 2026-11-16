package com.bloxbean.cardano.yaci.store.analytics.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DuckDbInstancesTest {

    private static final long GB = 1024L * 1024 * 1024;

    @TempDir
    Path tempDir;

    @Test
    void everyInstanceGetsItsOwnTempDirectoryUnderTheExportPath() throws Exception {
        AnalyticsStoreProperties properties = properties();

        try (DuckDbInstances.Instance writer = DuckDbInstances.open(properties, DuckDbInstances.Role.WRITER);
             DuckDbInstances.Instance reader = DuckDbInstances.open(properties, DuckDbInstances.Role.READER)) {
            Path writerTemp = Path.of(setting(writer.connection(), "temp_directory"));
            Path readerTemp = Path.of(setting(reader.connection(), "temp_directory"));

            Path base = tempDir.resolve("analytics/.tmp").toAbsolutePath().normalize();
            assertNotEquals(writerTemp, readerTemp);
            assertEquals(base, writerTemp.getParent());
            assertEquals(base, readerTemp.getParent());
            assertTrue(writerTemp.getFileName().toString().startsWith("writer-"), writerTemp.toString());
            assertTrue(readerTemp.getFileName().toString().startsWith("reader-"), readerTemp.toString());
        }
    }

    @Test
    void closingOneInstanceKeepsTheSpillFilesOfAnother() throws Exception {
        AnalyticsStoreProperties properties = properties();
        properties.getDuckdb().setMemoryLimit("64MB");

        try (DuckDbInstances.Instance a = DuckDbInstances.open(properties, DuckDbInstances.Role.WRITER)) {
            Path aTemp = a.tempDirectory();
            DuckDbInstances.Instance b = DuckDbInstances.open(properties, DuckDbInstances.Role.READER);
            Path bTemp = b.tempDirectory();

            spill(a.connection());
            spill(b.connection());
            assertTrue(spillFiles(aTemp) > 0, "instance A did not spill");
            assertTrue(spillFiles(bTemp) > 0, "instance B did not spill");
            long aFilesBefore = spillFiles(aTemp);

            b.close();

            assertFalse(Files.exists(bTemp), "closed instance's temp directory is removed");
            assertEquals(aFilesBefore, spillFiles(aTemp), "closing B must not delete A's spill files");
            try (Statement statement = a.connection().createStatement();
                 ResultSet rs = statement.executeQuery("SELECT count(*), sum(length(s)) FROM spill")) {
                rs.next();
                assertEquals(3_000_000L, rs.getLong(1));
                assertEquals(96_000_000L, rs.getLong(2));
            }
        }
    }

    @Test
    void appliesConfiguredSettingsToTheInstance() throws Exception {
        AnalyticsStoreProperties properties = properties();
        properties.getDuckdb().setMemoryLimit("128MB");
        properties.getDuckdb().setThreads(2);
        properties.getDuckdb().setMaxTempDirectorySize("1GB");
        properties.getDuckdb().setTempDirectory(tempDir.resolve("spill").toString());
        Path extensions = tempDir.resolve("ext");
        properties.getDuckdb().setExtensionDirectories(List.of(extensions.toString()));

        try (DuckDbInstances.Instance instance = DuckDbInstances.open(properties, DuckDbInstances.Role.WRITER)) {
            Connection c = instance.connection();
            assertEquals("122.0 MiB", setting(c, "memory_limit"));
            assertEquals("2", setting(c, "threads"));
            assertTrue(setting(c, "max_temp_directory_size").startsWith("953"), setting(c, "max_temp_directory_size"));
            assertEquals(tempDir.resolve("spill").toAbsolutePath().normalize(),
                    Path.of(setting(c, "temp_directory")).getParent());
            assertTrue(setting(c, "extension_directories").contains(extensions.toAbsolutePath().normalize().toString()));
            assertTrue(Files.isDirectory(extensions), "first extension directory is created");
        }
    }

    @Test
    void sweepRemovesDirectoriesOfDeadInstancesOnly() throws Exception {
        AnalyticsStoreProperties properties = properties();
        Path base = DuckDbInstances.tempDirectoryBase(properties);

        // Left behind by a crashed process: lock file present but not held.
        Path stale = Files.createDirectories(base.resolve("writer-dead0001"));
        Files.createFile(stale.resolve(DuckDbInstances.LOCK_FILE));
        Files.writeString(stale.resolve("duckdb_temp_storage_DEFAULT-0.tmp"), "stale");
        // Not ours: never touched.
        Path foreign = Files.createDirectories(base.resolve("something-else"));

        // Held by a "live" process: an exclusive lock that this JVM holds through another channel.
        Path live = Files.createDirectories(base.resolve("reader-live0001"));
        try (FileChannel channel = FileChannel.open(live.resolve(DuckDbInstances.LOCK_FILE),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock ignored = channel.lock();
             DuckDbInstances.Instance instance = DuckDbInstances.open(properties, DuckDbInstances.Role.WRITER)) {

            assertFalse(Files.exists(stale), "stale directory is removed when an instance opens");
            assertTrue(Files.exists(live), "a locked directory is kept");
            assertTrue(Files.exists(foreign));
            assertTrue(Files.exists(instance.tempDirectory()));

            assertEquals(0, DuckDbInstances.sweepStaleTempDirectories(base));
            assertTrue(Files.exists(instance.tempDirectory()), "own live directory is kept");
        }
    }

    @Test
    void defaultMemoryLimitSharesTheRamLeftAfterTheHeap() {
        // 16 GB RAM, 4 GB heap, writer + query layer: (16 - 4) * 0.5 / 2 = 3 GB each
        assertEquals("3072MB", DuckDbInstances.defaultMemoryLimit(16 * GB, 4 * GB, 2));
        // writer only: 6 GB
        assertEquals("6144MB", DuckDbInstances.defaultMemoryLimit(16 * GB, 4 * GB, 1));
        // tiny host: floor of 256 MB
        assertEquals("256MB", DuckDbInstances.defaultMemoryLimit(GB, GB, 2));
        // unknown RAM: keep DuckDB's default
        assertNull(DuckDbInstances.defaultMemoryLimit(-1, GB, 2));
    }

    @Test
    void configuredMemoryLimitWinsAndInstancesAreCounted() {
        AnalyticsStoreProperties properties = properties();
        properties.setEnabled(true);
        assertEquals(1, DuckDbInstances.longLivedInstanceCount(properties));
        properties.getQuery().setEnabled(true);
        assertEquals(2, DuckDbInstances.longLivedInstanceCount(properties));

        assertTrue(DuckDbInstances.effectiveMemoryLimit(properties, DuckDbInstances.Role.WRITER).endsWith("MB"));
        properties.getDuckdb().setMemoryLimit(" 2GB ");
        assertEquals("2GB", DuckDbInstances.effectiveMemoryLimit(properties, DuckDbInstances.Role.READER));
    }

    @Test
    void pooledConnectionCloseRemovesItsTempDirectory() throws Exception {
        AnalyticsStoreProperties properties = properties();
        javax.sql.DataSource dataSource = DuckDbInstances.dataSource(properties, DuckDbInstances.Role.WRITER);

        Path temp;
        try (Connection connection = dataSource.getConnection()) {
            temp = Path.of(setting(connection, "temp_directory"));
            assertTrue(Files.isDirectory(temp));
            assertTrue(connection.isWrapperFor(org.duckdb.DuckDBConnection.class));
        }
        assertFalse(Files.exists(temp));
    }

    private AnalyticsStoreProperties properties() {
        AnalyticsStoreProperties properties = new AnalyticsStoreProperties();
        properties.setExportPath(tempDir.resolve("analytics").toString());
        properties.getDuckdb().setThreads(1);
        return properties;
    }

    private static void spill(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE spill AS SELECT range i, md5(range::VARCHAR) s FROM range(3000000)");
        }
    }

    private static long spillFiles(Path dir) throws Exception {
        if (!Files.isDirectory(dir)) {
            return 0;
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(p -> p.getFileName().toString().startsWith("duckdb_temp")).count();
        }
    }

    static String setting(Connection connection, String name) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT current_setting('" + name + "')::VARCHAR")) {
            rs.next();
            return rs.getString(1);
        }
    }
}
