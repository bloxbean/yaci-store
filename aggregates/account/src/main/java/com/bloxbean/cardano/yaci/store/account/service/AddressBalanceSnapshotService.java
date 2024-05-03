package com.bloxbean.cardano.yaci.store.account.service;

import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.core.service.StartService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.impl.SQLDataType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.bloxbean.cardano.yaci.core.util.Constants.LOVELACE;
import static com.bloxbean.cardano.yaci.store.account.jooq.Tables.*;
import static com.bloxbean.cardano.yaci.store.account.jooq.Tables.STAKE_ADDRESS_BALANCE;
import static org.jooq.impl.DSL.*;

/**
 * Service to take balance snapshot for list of addresses
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AddressBalanceSnapshotService {
    private final EraService eraService;
    private final CursorService cursorService;
    private final StartService startService;
    private final DSLContext dsl;

    public synchronized boolean scheduleBalanceCalculationForAddresses(List<String> addresses, boolean isStakeAddress) {
        if (!startService.isStarted()) {
            log.info("Looks like sync process has not been started or balanced snapshot is in progress. Skipping balance snapshot");
            return false;
        }

        startService.stop();

        Thread.startVirtualThread(() -> {
            calculateBalance(addresses, isStakeAddress);
        });

        return true;
    }

    @SneakyThrows
    private boolean calculateBalance(List<String> addresses, boolean isStakeAddress) {
        log.info("Taking address balance snapshot ...");
        TimeUnit.SECONDS.sleep(5); //sleep for 5 seconds to allow the pending commit

        var cursor = cursorService.getCursor().orElse(null);

        if (cursor == null) {
            log.info("Cursor is null. Skipping address balance snapshot");
            return false;
        }

        if (isStakeAddress) {
            calculateStakeAddressBalance(cursor.getSlot(), addresses);
        } else {
            calculateAddressBalance(cursor.getSlot(), addresses);
        }

        log.info("Address balance snapshot completed. Starting the sync process ...");

        Thread.startVirtualThread(() -> {
            log.info("Waiting for 10 seconds before starting the sync process ...");
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
            }
            startService.start();
        });

        return true;
    }

    @Transactional
    public void calculateAddressBalance(long snapshotSlot, List<String> addresses) {
        var insertQuery = dsl.insertInto(ADDRESS_BALANCE,
                        ADDRESS_BALANCE.ADDRESS,
                        ADDRESS_BALANCE.UNIT,
                        ADDRESS_BALANCE.QUANTITY,
                        ADDRESS_BALANCE.SLOT,
                        ADDRESS_BALANCE.BLOCK,
                        ADDRESS_BALANCE.BLOCK_TIME,
                        ADDRESS_BALANCE.EPOCH,
                        ADDRESS_BALANCE.UPDATE_DATETIME
                ).select(select(field(ADDRESS_TX_AMOUNT.ADDRESS).as("address"),
                                field(ADDRESS_TX_AMOUNT.UNIT).as("unit"),
                                coalesce(sum(field(ADDRESS_TX_AMOUNT.QUANTITY)), BigDecimal.ZERO).cast(SQLDataType.DECIMAL_INTEGER(38)).as("quantity"),
                                max(field(ADDRESS_TX_AMOUNT.SLOT)).as("slot"),
                                max(field(ADDRESS_TX_AMOUNT.BLOCK)).as("block"),
                                max(field(ADDRESS_TX_AMOUNT.BLOCK_TIME)).as("block_time"),
                                max(field(ADDRESS_TX_AMOUNT.EPOCH)).as("epoch"),
                                currentLocalDateTime()
                        )
                                .from(ADDRESS_TX_AMOUNT)
                                .where(ADDRESS_TX_AMOUNT.SLOT.le(snapshotSlot)
                                        .and(ADDRESS_TX_AMOUNT.ADDRESS.in(addresses))
                                )
                                .groupBy(ADDRESS_TX_AMOUNT.ADDRESS, ADDRESS_TX_AMOUNT.UNIT)
                )
                .onConflict(
                        ADDRESS_BALANCE.ADDRESS,
                        ADDRESS_BALANCE.UNIT,
                        ADDRESS_BALANCE.SLOT
                )
                .doUpdate()
                .set(ADDRESS_BALANCE.QUANTITY, excluded(ADDRESS_BALANCE.QUANTITY))
                .set(ADDRESS_BALANCE.SLOT, excluded(ADDRESS_BALANCE.SLOT))
                .set(ADDRESS_BALANCE.BLOCK, excluded(ADDRESS_BALANCE.BLOCK))
                .set(ADDRESS_BALANCE.BLOCK_TIME, excluded(ADDRESS_BALANCE.BLOCK_TIME))
                .set(ADDRESS_BALANCE.EPOCH, excluded(ADDRESS_BALANCE.EPOCH));

        insertQuery.queryTimeout(300).execute();
    }

    @Transactional
    public void calculateStakeAddressBalance(long snapshotSlot, List<String> stakeAddresses) {
        var insertQuery = dsl.insertInto(STAKE_ADDRESS_BALANCE,
                        STAKE_ADDRESS_BALANCE.ADDRESS,
                        STAKE_ADDRESS_BALANCE.QUANTITY,
                        STAKE_ADDRESS_BALANCE.SLOT,
                        STAKE_ADDRESS_BALANCE.BLOCK,
                        STAKE_ADDRESS_BALANCE.BLOCK_TIME,
                        STAKE_ADDRESS_BALANCE.EPOCH,
                        STAKE_ADDRESS_BALANCE.UPDATE_DATETIME
                ).select(select(field(ADDRESS_TX_AMOUNT.STAKE_ADDRESS).as("address"),
                        coalesce(sum(field(ADDRESS_TX_AMOUNT.QUANTITY)), 0).cast(SQLDataType.DECIMAL_INTEGER(38)).as("quantity"),
                        max(field(ADDRESS_TX_AMOUNT.SLOT)).as("slot"),
                        max(field(ADDRESS_TX_AMOUNT.BLOCK)).as("block"),
                        max(field(ADDRESS_TX_AMOUNT.BLOCK_TIME)).as("block_time"),
                        max(field(ADDRESS_TX_AMOUNT.EPOCH)).as("epoch"),
                        currentLocalDateTime()
                )
                        .from(ADDRESS_TX_AMOUNT)
                        .where(ADDRESS_TX_AMOUNT.STAKE_ADDRESS.isNotNull()
                                .and(ADDRESS_TX_AMOUNT.SLOT.le(snapshotSlot))
                                .and(ADDRESS_TX_AMOUNT.STAKE_ADDRESS.in(stakeAddresses)
                                ).and(ADDRESS_TX_AMOUNT.UNIT.eq(LOVELACE))
                        )
                        .groupBy(ADDRESS_TX_AMOUNT.STAKE_ADDRESS))
                .onConflict(
                        STAKE_ADDRESS_BALANCE.ADDRESS,
                        STAKE_ADDRESS_BALANCE.SLOT
                )
                .doUpdate()
                .set(STAKE_ADDRESS_BALANCE.QUANTITY, excluded(STAKE_ADDRESS_BALANCE.QUANTITY))
                .set(STAKE_ADDRESS_BALANCE.SLOT, excluded(STAKE_ADDRESS_BALANCE.SLOT))
                .set(STAKE_ADDRESS_BALANCE.BLOCK, excluded(STAKE_ADDRESS_BALANCE.BLOCK))
                .set(STAKE_ADDRESS_BALANCE.BLOCK_TIME, excluded(STAKE_ADDRESS_BALANCE.BLOCK_TIME))
                .set(STAKE_ADDRESS_BALANCE.EPOCH, excluded(STAKE_ADDRESS_BALANCE.EPOCH));

        insertQuery.queryTimeout(300).execute();
    }
}
