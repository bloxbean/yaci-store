package com.bloxbean.cardano.yaci.store.dbutils.utxo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * One-time fill of {@code address_utxo_unspent} for a store that was synced before
 * {@code store.utxo.unspent-table-enabled} was turned on. The utxo store keeps the table current
 * from then on (UnspentUtxoTableService in stores/utxo, whose backfill and reconcile steps these
 * two statements mirror; this copy is plain SQL so the admin CLI needs no store modules).
 *
 * <p>Order of operations:
 * <ol>
 *   <li>turn {@code store.utxo.unspent-table-enabled} on and restart, so new commits are covered;</li>
 *   <li>{@link #tipSlot()}, then {@link #backfill} from slot 0 to that tip;</li>
 *   <li>{@link #reconcileSpentAfter} that tip: a chunk can read an output as unspent just before
 *       the indexer commits its spend;</li>
 *   <li>only then turn {@code store.utxo.unspent-table-read-enabled} on.</li>
 * </ol>
 * Every statement is idempotent, so the fill can be stopped and resumed.
 */
@Service
@Slf4j
public class UnspentUtxoBackfillService {

    static final String COLUMNS = "tx_hash, output_index, slot, block_hash, epoch, lovelace_amount, amounts, "
            + "data_hash, inline_datum, owner_addr, owner_addr_full, owner_stake_addr, owner_payment_credential, "
            + "owner_stake_credential, script_ref, reference_script_hash, is_collateral_return, block, block_time, "
            + "update_datetime, tx_index";

    static final String COPY_UNSPENT = "insert into address_utxo_unspent (" + COLUMNS + ") "
            + "select " + COLUMNS + " from address_utxo a "
            + "where a.slot between ? and ? "
            + "and not exists (select 1 from tx_input i where i.tx_hash = a.tx_hash and i.output_index = a.output_index) "
            + "and not exists (select 1 from address_utxo_unspent u where u.tx_hash = a.tx_hash and u.output_index = a.output_index)";

    static final String DELETE_SPENT_AFTER = "delete from address_utxo_unspent "
            + "where (tx_hash, output_index) in (select tx_hash, output_index from tx_input where spent_at_slot > ?)";

    private final JdbcTemplate jdbc;

    public UnspentUtxoBackfillService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The highest slot in address_utxo, or -1 for an empty store. */
    public long tipSlot() {
        Long slot = jdbc.queryForObject("select max(slot) from address_utxo", Long.class);
        return slot == null ? -1 : slot;
    }

    /**
     * Copies the unspent outputs with a slot between {@code fromSlot} and {@code toSlot}, one
     * autocommitted statement per {@code chunkSlots} slots.
     *
     * @return the rows copied
     */
    public long backfill(long fromSlot, long toSlot, long chunkSlots) {
        if (chunkSlots <= 0) {
            throw new IllegalArgumentException("chunkSlots must be positive");
        }
        long copied = 0;
        for (long start = fromSlot; start <= toSlot; start += chunkSlots) {
            long end = Math.min(start + chunkSlots - 1, toSlot);
            int rows = jdbc.update(COPY_UNSPENT, start, end);
            copied += rows;
            log.info("address_utxo_unspent backfill: slots {}..{} copied {} (total {})", start, end, rows, copied);
        }
        return copied;
    }

    /**
     * Deletes the rows spent after {@code slot}: the step after {@link #backfill}.
     *
     * @return the rows deleted
     */
    public int reconcileSpentAfter(long slot) {
        return jdbc.update(DELETE_SPENT_AFTER, slot);
    }
}
