package com.bloxbean.cardano.yaci.store.blockfrost.common.timeout;

import com.bloxbean.cardano.yaci.store.blockfrost.common.exception.BFDataAccessExceptionHandler;
import com.bloxbean.cardano.yaci.store.blockfrost.common.exception.BFGlobalExceptionHandler;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spring Data JPA + Hibernate repository calls through {@link BFQueryTimeoutDataSource}, wired by its post-processor
 * as in the application.
 */
@SpringBootTest(classes = BFQueryTimeoutJpaTest.Config.class,
        properties = "spring.jpa.hibernate.ddl-auto=create-drop")
public class BFQueryTimeoutJpaTest {

    private static final String COUNT_SQL = "select count(*) from bf_timeout_row";

    public static long sleep(long millis) throws InterruptedException {
        Thread.sleep(millis);
        return millis;
    }

    @Entity
    @Table(name = "bf_timeout_row")
    public static class Row {
        @Id
        public Long id;

        public Row() {
        }

        Row(Long id) {
            this.id = id;
        }
    }

    public interface RowRepository extends JpaRepository<Row, Long> {
        // Content query first, then a count query in the same repository call
        @Query(value = "select r.* from bf_timeout_row r cross join (select bf_sleep(?1) s) x order by r.id",
                countQuery = COUNT_SQL, nativeQuery = true)
        Page<Row> slowPage(long sleepMillis, Pageable pageable);

        // ~10^10 rows: runs for minutes unless the statement is cancelled
        @Query(value = "select count(*) from system_range(1, 100000) a cross join system_range(1, 100000) b "
                + "where a.x + b.x < 0", nativeQuery = true)
        long endless();
    }

    @RestController
    static class BfController {
        private final RowRepository repository;

        BfController(RowRepository repository) {
            this.repository = repository;
        }

        @GetMapping("/bf/endless")
        long endless() {
            return repository.endless();
        }

        @GetMapping("/bf/second-statement-after-deadline")
        long secondStatementAfterDeadline() {
            return repository.slowPage(1100, PageRequest.of(0, 1)).getTotalElements();
        }

        @GetMapping("/bf/swallowed")
        long swallowed() {
            try {
                return repository.endless();
            } catch (RuntimeException e) {
                return 0;
            }
        }
    }

    @Configuration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = BFQueryTimeoutJpaTest.class)
    @EnableJpaRepositories(basePackageClasses = BFQueryTimeoutJpaTest.class, considerNestedRepositories = true)
    static class Config {
        @Bean
        static BFQueryTimeoutDataSourcePostProcessor queryTimeoutDataSourcePostProcessor() {
            return new BFQueryTimeoutDataSourcePostProcessor();
        }

        // The recorder sits below the timeout wrapper, so it sees the timeout the driver gets
        @Bean
        DataSource dataSource() {
            JdbcDataSource h2 = new JdbcDataSource();
            h2.setURL("jdbc:h2:mem:bf_query_timeout_jpa;DB_CLOSE_DELAY=-1");
            HikariConfig config = new HikariConfig();
            config.setDataSource(RecordingDataSource.wrap(h2));
            return new HikariDataSource(config);
        }
    }

    @Autowired
    private RowRepository repository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("create alias if not exists bf_sleep for \""
                + BFQueryTimeoutJpaTest.class.getName() + ".sleep\"");
        repository.saveAll(List.of(new Row(1L), new Row(2L)));
        RecordingDataSource.reset();
    }

    @AfterEach
    void tearDown() {
        BFQueryTimeoutContext.clear();
        RecordingDataSource.reset();
    }

    private MockMvc mvc(Duration budget) {
        return MockMvcBuilders.standaloneSetup(new BfController(repository))
                .addInterceptors(new BFQueryTimeoutInterceptor(budget))
                .setControllerAdvice(new BFTimeoutResponseAdvice(), new BFDataAccessExceptionHandler(),
                        new BFGlobalExceptionHandler())
                .build();
    }

    @Test
    void mainDataSourceIsWrapped() {
        assertThat(dataSource).isInstanceOf(BFQueryTimeoutDataSource.class);
    }

    @Test
    void withoutBudget_statementHasNoTimeout() {
        repository.findAll();

        assertThat(RecordingDataSource.EXECUTED).singleElement()
                .satisfies(e -> assertThat(e.timeoutSeconds()).isZero());
    }

    @Test
    void withBudget_statementGetsRemainingTime() {
        BFQueryTimeoutContext.begin(Duration.ofSeconds(30));

        repository.findAll();

        assertThat(RecordingDataSource.EXECUTED).singleElement()
                .satisfies(e -> assertThat(e.timeoutSeconds()).isBetween(29, 30));
    }

    @Test
    void secondStatement_withUnder500msLeft_stillHasATimeout() {
        BFQueryTimeoutContext.begin(Duration.ofSeconds(1));

        Page<Row> page = repository.slowPage(700, PageRequest.of(0, 1));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(RecordingDataSource.last().sql()).isEqualTo(COUNT_SQL);
        assertThat(RecordingDataSource.last().timeoutSeconds()).isEqualTo(1);
    }

    @Test
    void secondStatement_afterDeadline_failsAsTimeoutWithoutRunning() {
        RecordingDataSource.forwardTimeouts = false;
        BFQueryTimeoutContext.begin(Duration.ofSeconds(1));

        assertThatThrownBy(() -> repository.slowPage(1100, PageRequest.of(0, 1)))
                .satisfies(e -> assertThat(BFQueryTimeouts.isTimeout(e)).isTrue());

        assertThat(RecordingDataSource.EXECUTED).noneSatisfy(e -> assertThat(e.sql()).isEqualTo(COUNT_SQL));
        assertThat(BFQueryTimeoutContext.isTimedOut()).isTrue();
    }

    @Test
    void longRunningStatement_isCancelledWhenBudgetRunsOut() {
        BFQueryTimeoutContext.begin(Duration.ofSeconds(1));

        long start = System.nanoTime();
        assertThatThrownBy(() -> repository.endless())
                .satisfies(e -> assertThat(BFQueryTimeouts.isTimeout(e)).isTrue());

        assertThat(Duration.ofNanos(System.nanoTime() - start).toMillis()).isLessThan(5_000);
        assertThat(BFQueryTimeoutContext.isTimedOut()).isTrue();
    }

    @Test
    void cancelledStatement_answers504() throws Exception {
        assertBlockfrost504(mvc(Duration.ofSeconds(1)), "/bf/endless");
    }

    @Test
    void secondStatementAfterDeadline_answers504() throws Exception {
        RecordingDataSource.forwardTimeouts = false;

        assertBlockfrost504(mvc(Duration.ofSeconds(1)), "/bf/second-statement-after-deadline");
    }

    @Test
    void timeoutCaughtByTheCaller_answers504() throws Exception {
        assertBlockfrost504(mvc(Duration.ofSeconds(1)), "/bf/swallowed");
    }

    private void assertBlockfrost504(MockMvc mvc, String path) throws Exception {
        mvc.perform(get(path))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.error").value("Gateway Timeout"))
                .andExpect(jsonPath("$.message").value(BFTimeoutResponseAdvice.MESSAGE))
                .andExpect(jsonPath("$.status_code").value(504));
    }
}
