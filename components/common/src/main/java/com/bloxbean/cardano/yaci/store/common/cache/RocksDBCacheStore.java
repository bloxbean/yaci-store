package com.bloxbean.cardano.yaci.store.common.cache;

import lombok.extern.slf4j.Slf4j;
import org.rocksdb.BlockBasedTableConfig;
import org.rocksdb.BloomFilter;
import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.ColumnFamilyOptions;
import org.rocksdb.CompressionType;
import org.rocksdb.DBOptions;
import org.rocksdb.LRUCache;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteOptions;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

/**
 * The one RocksDB of a Yaci Store process for caches, kept outside the database. Each store makes its own cache
 * with {@link #cache}, a column family of this RocksDB.
 *
 * <p>Only for data that can always be read again from the database: the folder is deleted when the store opens,
 * so every cache starts empty after a restart, and writes skip RocksDB's write-ahead log.
 *
 * <p>A call on a cache after {@link #close} throws an {@link IllegalStateException}: at shutdown the sync thread can
 * still be using a cache, and a RocksDB call on a closed handle crashes the JVM.
 */
@Slf4j
public class RocksDBCacheStore implements AutoCloseable {
    private static final long BLOCK_CACHE_BYTES = 128L * 1024 * 1024;

    private final Path folder;
    private final DBOptions dbOptions;
    private final LRUCache blockCache;
    private final ColumnFamilyOptions familyOptions;
    private final WriteOptions writeOptions;
    private final RocksDB db;
    private final List<ColumnFamilyHandle> handles = new ArrayList<>();
    //Calls hold the read lock, close the write lock, so close waits for the calls in progress
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private boolean closed;

    /** Deletes {@code folder} and opens an empty RocksDB there. */
    public RocksDBCacheStore(Path folder) {
        RocksDB.loadLibrary();
        this.folder = folder;
        deleteFolder(folder);
        try {
            Files.createDirectories(folder);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        dbOptions = new DBOptions().setCreateIfMissing(true);
        blockCache = new LRUCache(BLOCK_CACHE_BYTES);
        //Lookups are by whole key: a bloom filter per file skips the files that cannot hold it
        familyOptions = new ColumnFamilyOptions()
                .setCompressionType(CompressionType.LZ4_COMPRESSION)
                .setTableFormatConfig(new BlockBasedTableConfig()
                        .setBlockCache(blockCache)
                        .setFilterPolicy(new BloomFilter(10, false)));
        writeOptions = new WriteOptions().setDisableWAL(true);

        try {
            db = RocksDB.open(dbOptions, folder.toString(),
                    List.of(new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY, familyOptions)), handles);
        } catch (RocksDBException e) {
            throw new IllegalStateException("Could not open the RocksDB cache at " + folder, e);
        }
        log.info("RocksDB cache opened at {}", folder);
    }

    /**
     * A cache in its own column family. Call once per name: each call creates the column family.
     *
     * @param name a name unique in the process, such as "utxo_owner_addr"
     */
    public synchronized <K, V> RocksDBCache<K, V> cache(String name, RocksDBCache.Codec<K> keyCodec,
                                                       RocksDBCache.Codec<V> valueCodec) {
        ColumnFamilyHandle handle = call("create " + name, db -> db.createColumnFamily(
                new ColumnFamilyDescriptor(name.getBytes(StandardCharsets.UTF_8), familyOptions)));
        handles.add(handle);
        return new RocksDBCache<>(this, handle, keyCodec, valueCodec);
    }

    /** A RocksDB call, which fails with an {@link IllegalStateException} once the store is closed. */
    <T> T call(String operation, Call<T> call) {
        lock.readLock().lock();
        try {
            if (closed)
                throw new IllegalStateException("The RocksDB cache at " + folder + " is closed");
            return call.run(db);
        } catch (RocksDBException e) {
            throw new IllegalStateException("RocksDB cache " + operation + " failed", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    interface Call<T> {
        T run(RocksDB db) throws RocksDBException;
    }

    WriteOptions writeOptions() {
        return writeOptions;
    }

    @Override
    public synchronized void close() {
        lock.writeLock().lock();
        try {
            if (closed)
                return;
            closed = true;
        } finally {
            lock.writeLock().unlock();
        }
        handles.forEach(ColumnFamilyHandle::close);
        db.close();
        writeOptions.close();
        familyOptions.close();
        blockCache.close();
        dbOptions.close();
        log.info("RocksDB cache at {} closed", folder);
    }

    private static void deleteFolder(Path folder) {
        if (!Files.exists(folder))
            return;
        try (Stream<Path> paths = Files.walk(folder)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList())
                Files.delete(path);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not delete the RocksDB cache folder " + folder, e);
        }
    }
}
