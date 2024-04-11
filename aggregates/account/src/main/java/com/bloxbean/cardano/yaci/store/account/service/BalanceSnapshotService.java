package com.bloxbean.cardano.yaci.store.account.service;

import com.bloxbean.cardano.yaci.store.account.AccountStoreProperties;
import com.bloxbean.cardano.yaci.store.core.service.CursorService;
import com.bloxbean.cardano.yaci.store.core.service.StartService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class BalanceSnapshotService {
    private final CursorService cursorService;
    private final StartService startService;
    private final JdbcTemplate jdbcTemplate;
    private final AccountStoreProperties accountStoreProperties;
    private final EntityManager entityManager;

    /**
     * Schedule balance snapshot at current cursor block
     *
     * @return
     */
    @SneakyThrows
    public synchronized boolean scheduleBalanceSnapshot() {
        if (!startService.isStarted()) {
            log.info("Looks like sync process has not been started or balanced snapshot is in progress. Skipping balance snapshot");
            return false;
        }

        startService.stop();

        Thread.startVirtualThread(() -> {
            takeBalanceSnapshot();
        });

        return true;
    }

    /**
     * Take balance snapshot at a previous block
     *
     * @param block
     * @param slot
     * @param blockHash
     */
    @SneakyThrows
    public synchronized boolean scheduleBalanceSnapshot(long block, long slot, String blockHash) {
        log.info("Taking balance snapshot ...");

        var cursor = cursorService.getCursor().orElse(null);

        if (cursor == null) {
            log.info("Cursor is null. Skipping balance snapshot");
            return false;
        }

        if (block >= cursor.getBlock()) {
            log.warn("Block : {} is greater than or equal to current cursor : {}. Skipping balance snapshot", block, cursor.getBlock());
            return false;
        }

        Thread.startVirtualThread(() -> {
            takeBalanceSnapshot(block, slot);
        });

        return true;
    }

    /**
     * Take balance snapshot at current cursor block
     *
     * @return
     */
    @SneakyThrows
    private boolean takeBalanceSnapshot() {
        log.info("Taking balance snapshot ...");
        TimeUnit.SECONDS.sleep(5); //sleep for 5 seconds to allow the pending commit

        var cursor = cursorService.getCursor().orElse(null);

        if (cursor == null) {
            log.info("Cursor is null. Skipping balance snapshot");
            return false;
        }

        takeBalanceSnapshot(cursor.getBlock(), cursor.getSlot());
        return true;
    }

    @SneakyThrows
    private void takeBalanceSnapshot(long block, long slot) {
        //Take balance snapshot
        log.info("Trying to take balance snapshot at block : {}, slot: {}", block, slot);
        long startTime = System.currentTimeMillis();
        int gridSize = accountStoreProperties.getBalanceCalcJobPartitionSize();
        int batchSize = accountStoreProperties.getBalanceCalcJobBatchSize();
        long totalAddresses = jdbcTemplate.queryForObject("SELECT max(id) FROM address", Long.class);


        long partitionSize = totalAddresses / gridSize;

        runParallel(partitionSize, batchSize, gridSize, slot);

        log.info("Take snapshot at block : {}, slot: {}, take total time: [{} ms]", block, slot, System.currentTimeMillis() - startTime);

    }

    @SneakyThrows
    private void runParallel(long partitionSize, int batchSize, int gridSize, long totalAddresses) {

        List<CompletableFuture> futures = new ArrayList<>();
        for (int i = 0; i < gridSize; i++) {
            long startOffset = i * partitionSize;
            long endOffset = (i == gridSize - 1) ? totalAddresses : (startOffset + partitionSize);
            var completableFuture = CompletableFuture.supplyAsync(() -> {
                executeStoreProcedure(startOffset, endOffset, batchSize, totalAddresses);
                return true;
            });

            futures.add(completableFuture);
        }


        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        for (var future : futures) {
            future.get();
        }
    }

    private void executeStoreProcedure(long _from, long _to, long _batchSize, long _lastSnapshotSlot) {
        long startTime = System.currentTimeMillis();
        StoredProcedureQuery storedProcedure = entityManager.createStoredProcedureQuery("take_address_balance_snapshot");
        storedProcedure.registerStoredProcedureParameter("_from", Long.class, ParameterMode.IN);
        storedProcedure.registerStoredProcedureParameter("_to", Long.class, ParameterMode.IN);
        storedProcedure.registerStoredProcedureParameter("_batch_size", Long.class, ParameterMode.IN);
        storedProcedure.registerStoredProcedureParameter("_lastSnapshotSlot", Long.class, ParameterMode.IN);

        storedProcedure.setParameter("_from", _from);
        storedProcedure.setParameter("_to", _to);
        storedProcedure.setParameter("_batch_size", _batchSize);
        storedProcedure.setParameter("_lastSnapshotSlot", _lastSnapshotSlot);

        storedProcedure.execute();
        log.info("call take_address_balance_snapshot({},{},{}) with address [from:{} - to:{}], take[{} ms]", _from, _to, _lastSnapshotSlot, _from, _to, System.currentTimeMillis() - startTime);
    }

}
