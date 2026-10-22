package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.processor;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.springframework.stereotype.Component;

/**
 * Writes the first sighting of a CIP-68 user token with an upsert that the database resolves, not with a read followed by an insert.
 * <p>
 * Why: blocks can be processed in parallel (store.executor.enable-parallel-processing), each in its own transaction. Two transactions that
 * both see a token for the first time both read "not recorded" and both insert it, and the second fails with a duplicate key when it
 * <em>commits</em>, inside the block transaction, where an exception stops the whole sync (ADR 0004). That is what happened in the first
 * minutes of a preprod sync. A statement that resolves the conflict itself cannot fail that way.
 * <p>
 * The row keeps the <b>smallest</b> slot: with parallel processing a later block can commit first, and a rollback to a slot between the two must not
 * delete a token that was already seen before it. The dialect is known only at run time (PostgreSQL, MySQL, H2), hence one statement for each.
 */
@Component
public class Cip68UserTokenWriter {

    private static final String POSTGRESQL = "INSERT INTO cip68_user_token (unit, slot, tx_hash) VALUES (:unit, :slot, :txHash) "
            + "ON CONFLICT (unit) DO UPDATE SET slot = EXCLUDED.slot, tx_hash = EXCLUDED.tx_hash "
            + "WHERE EXCLUDED.slot < cip68_user_token.slot";

    // MySQL evaluates the assignments from left to right: tx_hash is set while slot still holds the old value
    private static final String MYSQL = "INSERT INTO cip68_user_token (unit, slot, tx_hash) VALUES (:unit, :slot, :txHash) "
            + "ON DUPLICATE KEY UPDATE tx_hash = IF(VALUES(slot) < slot, VALUES(tx_hash), tx_hash), slot = LEAST(slot, VALUES(slot))";

    private static final String H2 = "MERGE INTO cip68_user_token t USING (VALUES (CAST(:unit AS VARCHAR(120)), CAST(:slot AS BIGINT), CAST(:txHash AS VARCHAR(64)))) "
            + "s (unit, slot, tx_hash) ON t.unit = s.unit "
            + "WHEN MATCHED AND s.slot < t.slot THEN UPDATE SET slot = s.slot, tx_hash = s.tx_hash "
            + "WHEN NOT MATCHED THEN INSERT (unit, slot, tx_hash) VALUES (s.unit, s.slot, s.tx_hash)";

    @PersistenceContext
    private EntityManager entityManager;

    private volatile String statement;

    /** Records the token, or lowers the slot of the row when this sighting is earlier. Needs a transaction. */
    public void upsertFirstSeen(String unit, String txHash, long slot) {
        entityManager.createNativeQuery(statement())
                .setParameter("unit", unit)
                .setParameter("slot", slot)
                .setParameter("txHash", txHash)
                .executeUpdate();
    }

    private String statement() {
        String sql = statement;
        if (sql == null) {
            Dialect dialect = entityManager.getEntityManagerFactory().unwrap(SessionFactoryImplementor.class).getJdbcServices().getDialect();
            if (dialect instanceof PostgreSQLDialect) {
                sql = POSTGRESQL;
            } else if (dialect instanceof MySQLDialect) {
                sql = MYSQL;
            } else if (dialect instanceof H2Dialect) {
                sql = H2;
            } else {
                throw new IllegalStateException("No upsert of the CIP-68 user tokens for the database dialect " + dialect.getClass().getName());
            }
            statement = sql;
        }
        return sql;
    }
}
