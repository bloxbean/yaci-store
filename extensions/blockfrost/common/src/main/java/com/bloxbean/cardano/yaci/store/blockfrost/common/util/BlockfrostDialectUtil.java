package com.bloxbean.cardano.yaci.store.blockfrost.common.util;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;

public final class BlockfrostDialectUtil {
    private BlockfrostDialectUtil() {
    }

    public static boolean isPostgres(DSLContext dslContext) {
        return dslContext != null && isPostgres(dslContext.dialect());
    }

    public static boolean isPostgres(SQLDialect dialect) {
        return dialect != null && dialect.family() == SQLDialect.POSTGRES;
    }
}
