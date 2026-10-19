package com.bloxbean.cardano.yaci.store.dbutils.utxo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builds {@code address_utxo_unspent} from {@code address_utxo} minus {@code tx_input}, for a store
 * synced before {@code store.utxo.unspent-table-enabled} was turned on. The utxo store keeps the table
 * current from then on (UtxoStorageImpl in stores/utxo); these statements are plain SQL so the admin
 * CLI needs no store modules.
 *
 * <p>The store must be stopped for the whole run: the table is then exact at the store's cursor, and
 * the store continues from that point once it starts with {@code store.utxo.unspent-table-enabled=true}.
 * {@link #cursorSlot()} lets the caller check that the cursor did not move while the build ran.
 *
 * <p>The unspent outputs are found one {@code output_index} at a time, in {@link #SLICE_BOUNDS
 * tx_hash slices}. Both tables have the primary key {@code (output_index, tx_hash)}, so each slice is
 * an anti-join of two ordered key ranges that reads no row data. Only the outputs found unspent, a
 * few percent of {@code address_utxo} on mainnet, are then copied with their columns.
 */
@Service
@Slf4j
public class UnspentUtxoBackfillService {

    public static final String COLUMNS = "tx_hash, output_index, slot, block_hash, epoch, lovelace_amount, amounts, "
            + "data_hash, inline_datum, owner_addr, owner_addr_full, owner_stake_addr, owner_payment_credential, "
            + "owner_stake_credential, script_ref, reference_script_hash, is_collateral_return, block, block_time, "
            + "update_datetime, tx_index";

    /**
     * Rollback of the table to {@code :slot}, after its rows with a later slot are deleted: the outputs created at
     * or before the point whose spend is rolled back become unspent again. Must run before tx_input is trimmed.
     */
    public static final String RESTORE_SPENT_AFTER = "insert into address_utxo_unspent (" + COLUMNS + ") "
            + "select " + COLUMNS.replaceAll("(\\w+)", "a.$1") + " "
            + "from tx_input t join address_utxo a on a.tx_hash = t.tx_hash and a.output_index = t.output_index "
            + "where t.spent_at_slot > :slot and a.slot <= :slot "
            + "and not exists (select 1 from address_utxo_unspent u where u.tx_hash = a.tx_hash and u.output_index = a.output_index)";

    /** Lower bounds of the tx_hash slices of one output_index; the last slice has no upper bound. */
    static final String[] SLICE_BOUNDS = {"", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"};

    /** The inner range repeats the outer one so the anti-join reads only this slice of tx_input too. */
    private static final String UNSPENT_KEYS = "select a.tx_hash from address_utxo a "
            + "where a.output_index = ? and a.tx_hash >= ? %1$s "
            + "and not exists (select 1 from tx_input i where i.output_index = a.output_index and i.tx_hash = a.tx_hash "
            + "and i.tx_hash >= ? %2$s)";

    static final int COPY_BATCH_SIZE = 1000;

    private static final long PROGRESS_LOG_INTERVAL_MS = 10_000;

    private final JdbcTemplate jdbc;

    public UnspentUtxoBackfillService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The highest slot in cursor_, or null when the store has not synced a block. */
    public Long cursorSlot() {
        return jdbc.queryForObject("select max(slot) from cursor_", Long.class);
    }

    /** The highest output_index in address_utxo, or -1 for an empty store. */
    public int maxOutputIndex() {
        Integer index = jdbc.queryForObject("select max(output_index) from address_utxo", Integer.class);
        return index == null ? -1 : index;
    }

    /**
     * Fills the table with every unspent output whose output_index is {@code fromOutputIndex} or higher.
     * From 0 the table is emptied first; from a later index (to resume an interrupted run) only the rows
     * from that index on are removed, so the run continues where it stopped.
     *
     * @return the rows copied
     */
    public long rebuild(int fromOutputIndex) {
        if (fromOutputIndex < 0) {
            throw new IllegalArgumentException("fromOutputIndex must not be negative");
        }
        if (fromOutputIndex == 0) {
            jdbc.execute("truncate table address_utxo_unspent");
        } else {
            jdbc.update("delete from address_utxo_unspent where output_index >= ?", fromOutputIndex);
        }

        int maxOutputIndex = maxOutputIndex();
        long copied = 0;
        long lastLog = System.currentTimeMillis();
        for (int outputIndex = fromOutputIndex; outputIndex <= maxOutputIndex; outputIndex++) {
            for (int slice = 0; slice < SLICE_BOUNDS.length; slice++) {
                copied += copy(outputIndex, unspentKeys(outputIndex, slice));
            }
            if (System.currentTimeMillis() - lastLog >= PROGRESS_LOG_INTERVAL_MS || outputIndex == maxOutputIndex) {
                log.info("address_utxo_unspent build: output_index {} of {} done, {} rows copied", outputIndex, maxOutputIndex, copied);
                lastLog = System.currentTimeMillis();
            }
        }
        return copied;
    }

    private List<String> unspentKeys(int outputIndex, int slice) {
        String lower = SLICE_BOUNDS[slice];
        if (slice == SLICE_BOUNDS.length - 1) {
            return jdbc.queryForList(UNSPENT_KEYS.formatted("", ""), String.class, outputIndex, lower, lower);
        }
        String upper = SLICE_BOUNDS[slice + 1];
        return jdbc.queryForList(UNSPENT_KEYS.formatted("and a.tx_hash < ?", "and i.tx_hash < ?"), String.class,
                outputIndex, lower, upper, lower, upper);
    }

    private long copy(int outputIndex, List<String> txHashes) {
        long copied = 0;
        for (int from = 0; from < txHashes.size(); from += COPY_BATCH_SIZE) {
            List<String> batch = txHashes.subList(from, Math.min(from + COPY_BATCH_SIZE, txHashes.size()));
            String placeholders = String.join(", ", Collections.nCopies(batch.size(), "?"));
            List<Object> args = new ArrayList<>(batch.size() + 1);
            args.add(outputIndex);
            args.addAll(batch);
            copied += jdbc.update("insert into address_utxo_unspent (" + COLUMNS + ") select " + COLUMNS
                    + " from address_utxo where output_index = ? and tx_hash in (" + placeholders + ")", args.toArray());
        }
        return copied;
    }
}
