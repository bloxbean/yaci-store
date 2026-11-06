package com.bloxbean.cardano.yaci.store.blockfrost.common.timeout;

import com.bloxbean.cardano.yaci.store.blockfrost.common.exception.BFDataAccessExceptionHandler;
import com.bloxbean.cardano.yaci.store.blockfrost.common.exception.BFGlobalExceptionHandler;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Repository calls on a real Spring Data JPA + Hibernate stack, recording the timeout each statement runs with.
 */
@SpringBootTest(classes = BFRepositoryTimeoutJpaTest.Config.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:bf_repository_timeout;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"})
public class BFRepositoryTimeoutJpaTest {

    private static final String COUNT_SQL = "select count(*) from bf_timeout_row";

    record Executed(String sql, int timeoutSeconds) {
    }

    private static final List<Executed> EXECUTED = new CopyOnWriteArrayList<>();
    // Off: a statement overruns uncancelled, like a query that finishes right at the deadline
    private static volatile boolean forwardTimeouts = true;

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
    @EntityScan(basePackageClasses = BFRepositoryTimeoutJpaTest.class)
    @EnableJpaRepositories(basePackageClasses = BFRepositoryTimeoutJpaTest.class, considerNestedRepositories = true)
    static class Config {
        @Bean
        static BFRepositoryTimeoutPostProcessor repositoryTimeoutPostProcessor(
                ObjectProvider<PlatformTransactionManager> transactionManager,
                ObjectProvider<EntityManagerFactory> entityManagerFactory) {
            return new BFRepositoryTimeoutPostProcessor(
                    new BFRepositoryTimeoutInterceptor(transactionManager, entityManagerFactory));
        }

        @Bean
        static BeanPostProcessor statementRecorder() {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String beanName) {
                    return bean instanceof DataSource dataSource ? recording(DataSource.class, dataSource) : bean;
                }
            };
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T recording(Class<T> type, T target) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            Object result = invoke(target, method, args);
            if (result instanceof Connection connection && type == DataSource.class)
                return recording(Connection.class, connection);
            if (result instanceof PreparedStatement statement && method.getName().equals("prepareStatement"))
                return recordingStatement(statement, (String) args[0]);
            return result;
        });
    }

    private static PreparedStatement recordingStatement(PreparedStatement target, String sql) {
        int[] timeout = {0};
        return (PreparedStatement) Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),
                new Class<?>[]{PreparedStatement.class}, (proxy, method, args) -> {
                    if (method.getName().equals("setQueryTimeout")) {
                        timeout[0] = (int) args[0];
                        if (!forwardTimeouts)
                            return null;
                    }
                    if (method.getName().startsWith("execute"))
                        EXECUTED.add(new Executed(sql, timeout[0]));
                    return invoke(target, method, args);
                });
    }

    private static Object invoke(Object target, java.lang.reflect.Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Autowired
    private RowRepository repository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("create alias if not exists bf_sleep for \""
                + BFRepositoryTimeoutJpaTest.class.getName() + ".sleep\"");
        repository.saveAll(List.of(new Row(1L), new Row(2L)));
        EXECUTED.clear();
        forwardTimeouts = true;
    }

    @AfterEach
    void tearDown() {
        BFQueryTimeoutContext.clear();
        forwardTimeouts = true;
    }

    private Executed lastExecuted() {
        return EXECUTED.get(EXECUTED.size() - 1);
    }

    private MockMvc mvc(Duration budget) {
        return MockMvcBuilders.standaloneSetup(new BfController(repository))
                .addInterceptors(new BFQueryTimeoutInterceptor(budget))
                .setControllerAdvice(new BFTimeoutResponseAdvice(), new BFDataAccessExceptionHandler(),
                        new BFGlobalExceptionHandler())
                .build();
    }

    @Test
    void withoutBudget_statementHasNoTimeout() {
        repository.findAll();

        assertThat(EXECUTED).singleElement().satisfies(e -> assertThat(e.timeoutSeconds()).isZero());
    }

    @Test
    void withBudget_statementGetsRemainingTime() {
        BFQueryTimeoutContext.begin(Duration.ofSeconds(30));

        repository.findAll();

        assertThat(EXECUTED).singleElement().satisfies(e -> assertThat(e.timeoutSeconds()).isBetween(29, 30));
    }

    @Test
    void secondStatement_withUnder500msLeft_stillHasATimeout() {
        BFQueryTimeoutContext.begin(Duration.ofSeconds(1));

        Page<Row> page = repository.slowPage(700, PageRequest.of(0, 1));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(lastExecuted().sql()).isEqualTo(COUNT_SQL);
        assertThat(lastExecuted().timeoutSeconds()).isEqualTo(1);
    }

    @Test
    void secondStatement_afterDeadline_failsAsTimeoutWithoutRunning() {
        forwardTimeouts = false;
        BFQueryTimeoutContext.begin(Duration.ofSeconds(1));

        assertThatThrownBy(() -> repository.slowPage(1100, PageRequest.of(0, 1)))
                .isInstanceOf(QueryTimeoutException.class);

        assertThat(EXECUTED).noneSatisfy(e -> assertThat(e.sql()).isEqualTo(COUNT_SQL));
        assertThat(BFQueryTimeoutContext.isTimedOut()).isTrue();
    }

    @Test
    void longRunningStatement_isCancelledWhenBudgetRunsOut() {
        BFQueryTimeoutContext.begin(Duration.ofSeconds(1));

        long start = System.nanoTime();
        assertThatThrownBy(() -> repository.endless()).isInstanceOf(QueryTimeoutException.class);

        assertThat(Duration.ofNanos(System.nanoTime() - start).toMillis()).isLessThan(5_000);
        assertThat(BFQueryTimeoutContext.isTimedOut()).isTrue();
    }

    @Test
    void cancelledStatement_answers504() throws Exception {
        assertBlockfrost504(mvc(Duration.ofSeconds(1)), "/bf/endless");
    }

    @Test
    void secondStatementAfterDeadline_answers504() throws Exception {
        forwardTimeouts = false;

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
