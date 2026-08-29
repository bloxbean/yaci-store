package com.bloxbean.cardano.yaci.store.snapshot.handler;

import com.bloxbean.cardano.yaci.store.snapshot.util.Identifiers;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Rebuilds {@code era} from the first imported block of each era.
 *
 * <p>Yaci Store writes an era row the first time it sees a block of that era, recording the block's
 * slot, number and hash. Deriving the same rows from the imported block table reproduces exactly
 * that, and {@code EraService} then resolves the Shelley start slot the way it always does.
 *
 * <p>Byron is excluded. A normally synced database holds one row per era from the first non-Byron
 * era onward, and {@code EraService.getEraForEpoch()} walks {@code findAllEras()} calling
 * {@code getEpochNo()} on each row -- which throws for a Byron row, because an epoch cannot be
 * derived from a Byron slot. Writing a Byron row therefore breaks the AdaPot reward calculation at
 * the first epoch boundary after a restore.
 */
public class EraTransitionHandler implements SnapshotHandler {

    public static final String NAME = "era-transitions";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String apply(Context ctx) throws SQLException {
        String schema = Identifiers.quote(ctx.schema());
        String era = schema + "." + Identifiers.quote("era");
        String block = schema + "." + Identifiers.quote("block");

        try (Statement st = ctx.connection().createStatement()) {
            st.executeUpdate("DELETE FROM " + era);
        }

        String insert = "INSERT INTO " + era + " (era, start_slot, block, block_hash)"
                + " SELECT era, slot, number, hash FROM ("
                + "   SELECT b.era, b.slot, b.number, b.hash,"
                + "          row_number() OVER (PARTITION BY b.era ORDER BY b.number) AS rn"
                + "   FROM " + block + " b"
                + "   WHERE b.number >= 0 AND b.number <= ? AND b.era IS NOT NULL AND b.era > 1"
                + " ) first_of_era WHERE rn = 1";
        int inserted;
        try (PreparedStatement ps = ctx.connection().prepareStatement(insert)) {
            ps.setLong(1, ctx.point().blockNumber());
            inserted = ps.executeUpdate();
        }
        if (inserted == 0) {
            throw new SQLException("No era rows could be reconstructed; is the block table empty?");
        }
        return "reconstructed " + inserted + " era transition(s)";
    }
}
