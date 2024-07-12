package com.bloxbean.cardano.yaci.store.account.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.impl.SQLDataType;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import static com.bloxbean.cardano.yaci.store.account.job.AccountJobConstants.STAKE_ADDRESS_TEMP_TABLE;

@RequiredArgsConstructor
@Slf4j
public class CreateStakeAddressTableTasklet implements Tasklet {
    private final DSLContext dsl;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("<< Creating temp table for stake addresses >> ");
        dsl.dropTableIfExists(STAKE_ADDRESS_TEMP_TABLE).execute();
        dsl.createTable(STAKE_ADDRESS_TEMP_TABLE)
            .column("id", SQLDataType.BIGINT.identity(true))
            .column("address", SQLDataType.VARCHAR(255))
            .execute();

        String insertQuery = String.format("""
                insert into %s (address)
                    select stake_address as address
                        from address
                        where stake_address is not null
                        group by stake_address;
                """, STAKE_ADDRESS_TEMP_TABLE);

        log.info("Inserting stake addresses into temp table >>>");
        dsl.execute(insertQuery);

        log.info("<< Temp table created for stake addresses >>");

        log.info("<< Creating index on address_utxo >> ");
        String addrIndexQuery = "CREATE INDEX if not exists idx_address_utxo_owner_addr ON address_utxo(owner_addr)";
        String stakeAddrIndexQuery = "CREATE INDEX if not exists idx_address_utxo_owner_stake_addr ON address_utxo(owner_stake_addr)";

        dsl.batch(addrIndexQuery, stakeAddrIndexQuery).execute();
        log.info("<< Index created on address_utxo >>");

        return RepeatStatus.FINISHED;
    }
}
