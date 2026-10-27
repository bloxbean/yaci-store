package com.bloxbean.cardano.yaci.store.snapshot.handler;

import com.bloxbean.cardano.yaci.store.snapshot.util.Identifiers;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Seeds {@code adapot_jobs} so completed reward history is not recalculated after a restore.
 *
 * <p>The ADR is explicit that an absent {@code adapot_jobs} row must not be read as proof that
 * AdaPot state is valid: with no rows, the aggregate would try to recompute every historical epoch
 * against data it no longer has. One {@code REWARD_CALC / COMPLETED} row is therefore written per
 * epoch that the snapshot actually carries, up to and including the snapshot epoch.
 *
 * <p>Nothing is written beyond the snapshot epoch, so the first epoch after the restore point is
 * still calculated normally.
 */
public class AdaPotJobsHandler implements SnapshotHandler {

    public static final String NAME = "adapot-jobs";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String apply(Context ctx) throws SQLException {
        String schema = Identifiers.quote(ctx.schema());
        String jobs = schema + "." + Identifiers.quote("adapot_jobs");
        String adapot = schema + "." + Identifiers.quote("adapot");

        try (Statement st = ctx.connection().createStatement()) {
            st.executeUpdate("DELETE FROM " + jobs);
        }

        // adapot holds one row per epoch whose reward calculation completed, with the slot and block
        // the calculation ran at, which is exactly the job's identity.
        String insert = "INSERT INTO " + jobs + " (epoch, slot, block, block_hash, type, status)"
                + " SELECT a.epoch, a.slot, b.number, b.hash, 'REWARD_CALC', 'COMPLETED'"
                + " FROM " + adapot + " a"
                + " LEFT JOIN " + schema + "." + Identifiers.quote("block") + " b ON b.slot = a.slot"
                + " WHERE a.epoch <= ?";
        int inserted;
        try (PreparedStatement ps = ctx.connection().prepareStatement(insert)) {
            ps.setInt(1, ctx.point().epoch());
            inserted = ps.executeUpdate();
        }
        return "seeded " + inserted + " completed AdaPot job(s) up to epoch " + ctx.point().epoch();
    }
}
