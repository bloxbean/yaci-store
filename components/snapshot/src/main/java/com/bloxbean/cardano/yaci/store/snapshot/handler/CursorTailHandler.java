package com.bloxbean.cardano.yaci.store.snapshot.handler;

import com.bloxbean.cardano.yaci.store.snapshot.util.Identifiers;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Rebuilds {@code cursor_} from the imported block tail.
 *
 * <p>Seeding only the final cursor would be enough to start, but not enough to survive the first
 * rollback: {@code CursorServiceImpl.getStartCursor()} walks backwards through previous cursors when
 * the node rejects a point. The tail is therefore seeded to the configured
 * {@code store.cardano.cursor-no-of-blocks-to-keep} depth, ending exactly at the snapshot point.
 *
 * <p>Rows are taken from the imported {@code block} table rather than from Parquet, which guarantees
 * the invariant validation checks: the final cursor is an imported block.
 */
public class CursorTailHandler implements SnapshotHandler {

    public static final String NAME = "cursor-tail";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String apply(Context ctx) throws SQLException {
        String schema = Identifiers.quote(ctx.schema());
        String cursor = schema + "." + Identifiers.quote("cursor_");
        String block = schema + "." + Identifiers.quote("block");

        try (Statement st = ctx.connection().createStatement()) {
            st.executeUpdate("DELETE FROM " + cursor + " WHERE id = " + ctx.eventPublisherId());
        }

        int keep = Math.max(1, ctx.cursorBlocksToKeep());
        String insert = "INSERT INTO " + cursor
                + " (id, block_hash, slot, block_number, era, prev_block_hash, create_datetime, update_datetime)"
                + " SELECT ?, b.hash, b.slot, b.number, b.era, b.prev_hash, now(), now()"
                + " FROM " + block + " b"
                + " WHERE b.number <= ? AND b.number >= 0"
                + " ORDER BY b.number DESC"
                + " LIMIT ?";
        int inserted;
        try (PreparedStatement ps = ctx.connection().prepareStatement(insert)) {
            ps.setLong(1, ctx.eventPublisherId());
            ps.setLong(2, ctx.point().blockNumber());
            ps.setInt(3, keep);
            inserted = ps.executeUpdate();
        }

        // The final cursor must be the manifest point, not merely the newest row that happened to load.
        try (PreparedStatement ps = ctx.connection().prepareStatement(
                "SELECT block_hash, slot, block_number FROM " + cursor
                        + " WHERE id = ? ORDER BY slot DESC LIMIT 1")) {
            ps.setLong(1, ctx.eventPublisherId());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("No cursor rows could be seeded; is the block table empty?");
                }
                String hash = rs.getString(1);
                long slot = rs.getLong(2);
                long number = rs.getLong(3);
                if (!hash.equals(ctx.point().blockHash()) || slot != ctx.point().slot()
                        || number != ctx.point().blockNumber()) {
                    throw new SQLException("Final cursor (block " + number + ", slot " + slot
                            + ") does not match the manifest point (block " + ctx.point().blockNumber()
                            + ", slot " + ctx.point().slot() + ")");
                }
            }
        }
        return "seeded " + inserted + " cursor row(s) ending at block " + ctx.point().blockNumber()
                + " for event publisher " + ctx.eventPublisherId();
    }
}
