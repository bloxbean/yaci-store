package com.bloxbean.cardano.yaci.store.snapshot.handler;

import com.bloxbean.cardano.yaci.store.snapshot.util.Identifiers;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Seeds the account module's balance watermark at the snapshot point.
 *
 * <p>{@code AccountBalanceProcessor} refuses to calculate balances unless the block it is asked to
 * process follows the one recorded in {@code account_config}. On an empty table that watermark reads
 * as block 0, so after a restore the processor sees a multi-million block gap and skips every
 * balance calculation -- which in turn starves the AdaPot stake snapshot and fails reward
 * calculation at the first epoch boundary.
 *
 * <p>The snapshot point is exactly the block up to which account data was imported, so it is the
 * correct watermark.
 */
public class AccountConfigHandler implements SnapshotHandler {

    public static final String NAME = "account-config";

    /** Mirrors {@code com.bloxbean.cardano.yaci.store.account.util.ConfigIds}. */
    static final String LAST_ACCOUNT_BALANCE_PROCESSED_BLOCK = "last_account_balance_processed_block";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String apply(Context ctx) throws SQLException {
        String table = Identifiers.quote(ctx.schema()) + "." + Identifiers.quote("account_config");

        try (Statement st = ctx.connection().createStatement()) {
            st.executeUpdate("DELETE FROM " + table + " WHERE config_id = '"
                    + LAST_ACCOUNT_BALANCE_PROCESSED_BLOCK + "'");
        }
        try (PreparedStatement ps = ctx.connection().prepareStatement(
                "INSERT INTO " + table + " (config_id, slot, block, block_hash) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, LAST_ACCOUNT_BALANCE_PROCESSED_BLOCK);
            ps.setLong(2, ctx.point().slot());
            ps.setLong(3, ctx.point().blockNumber());
            ps.setString(4, ctx.point().blockHash());
            ps.executeUpdate();
        }
        return "account balance watermark set to block " + ctx.point().blockNumber()
                + " (slot " + ctx.point().slot() + ")";
    }
}
