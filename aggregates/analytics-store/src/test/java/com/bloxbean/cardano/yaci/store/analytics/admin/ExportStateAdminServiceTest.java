package com.bloxbean.cardano.yaci.store.analytics.admin;

import com.bloxbean.cardano.yaci.store.analytics.state.ExportState;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateId;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateRepository;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(ExportStateAdminService.class)
@TestPropertySource(properties = {
        "yaci.store.analytics.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:adminstatetest",
        "spring.datasource.username=sa",
        "spring.datasource.password=password",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ExportStateAdminServiceTest {

    @Autowired
    private ExportStateAdminService service;

    @Autowired
    private ExportStateRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        for (String day : new String[]{"2023-12-31", "2024-01-01", "2024-01-15", "2024-01-31", "2024-02-01"}) {
            save("transaction", "date=" + day);
        }
        save("transaction", "date=1970-01-01");
        save("other_table", "date=2024-01-15");
        save("reward", "epoch=450");
        save("reward", "epoch=-2147483648");
    }

    @Test
    void resetsTheDaysOfTheRangeGivenAsPlainDates() {
        int count = service.resetDateRange("transaction", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));

        assertThat(count).isEqualTo(3);
        assertThat(keys("transaction")).containsExactlyInAnyOrder("date=2023-12-31", "date=2024-02-01",
                "date=1970-01-01");
        assertThat(keys("other_table")).containsExactly("date=2024-01-15");
    }

    @Test
    void aRangeWithoutStateResetsNothing() {
        assertThat(service.resetDateRange("transaction", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31)))
                .isZero();
        assertThat(keys("transaction")).hasSize(6);
    }

    @Test
    void rejectsAReversedRange() {
        assertThatThrownBy(() -> service.resetDateRange("transaction", LocalDate.of(2024, 2, 1),
                LocalDate.of(2024, 1, 1))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resetsOnePartitionWrittenEitherWay() {
        assertThat(service.resetExportState("transaction", "2024-01-15")).isTrue();
        assertThat(service.resetExportState("transaction", "date=2024-01-31")).isTrue();
        assertThat(service.resetExportState("reward", "450")).isTrue();
        assertThat(service.resetExportState("reward", "epoch=-2147483648")).isTrue();

        assertThat(keys("transaction")).doesNotContain("date=2024-01-15", "date=2024-01-31");
        assertThat(keys("reward")).isEmpty();
    }

    @Test
    void reportsAPartitionWithoutState() {
        assertThat(service.resetExportState("transaction", "2024-10-31")).isFalse();
        assertThat(service.resetExportState("unknown_table", "2024-01-15")).isFalse();
        assertThatThrownBy(() -> service.resetExportState("transaction", "~"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void statisticsCountOnlyTheTable() {
        assertThat(service.getStatistics("reward").getTotalPartitions()).isEqualTo(2);
    }

    private java.util.List<String> keys(String table) {
        return repository.findByTableName(table).stream().map(s -> s.getId().getPartitionValue()).toList();
    }

    private void save(String table, String key) {
        ExportState state = new ExportState(table, key);
        state.setStatus(ExportStatus.COMPLETED);
        state.setRowCount(1L);
        repository.save(state);
        assertThat(repository.findById(new ExportStateId(table, key))).isPresent();
    }
}
