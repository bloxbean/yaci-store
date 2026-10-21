package com.bloxbean.cardano.yaci.store.admin.cli.snapshot;

import com.bloxbean.cardano.yaci.store.snapshot.util.Progress;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ProgressPrinterTest {

    @Test
    void numbersBannersIndentsStatusLinesAndDropsPerItemEvents() {
        ProgressPrinter printer = new ProgressPrinter(6);
        assertThat(printer.format(Progress.PHASE_PREFIX + "Verifying and extracting 18 parts (140.8 GiB)"))
                .endsWith("[1/6] Verifying and extracting 18 parts (140.8 GiB)");
        assertThat(printer.format("[extract] 3/18 parts")).isEqualTo("      [extract] 3/18 parts");
        assertThat(printer.format("address_utxo +1200 rows")).isNull();
        assertThat(printer.format(Progress.PHASE_PREFIX + "Loading 10 rows in 2 batches"))
                .endsWith("[2/6] Loading 10 rows in 2 batches");
    }

    @Test
    void nestedBannersBecomeTaggedStatusLines() {
        PrintStream original = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
        try {
            ProgressPrinter printer = new ProgressPrinter(2);
            printer.accept(Progress.PHASE_PREFIX + "Validation");
            printer.nested("validate").accept(Progress.PHASE_PREFIX + "Semantic checks");
            printer.nested("validate").accept("[validate] Scanned 3 table(s) in 0m01s");
        } finally {
            System.setOut(original);
        }
        assertThat(out.toString(StandardCharsets.UTF_8).lines().toList())
                .hasSize(3)
                .satisfies(lines -> {
                    assertThat(lines.get(0)).endsWith("[1/2] Validation");
                    assertThat(lines.get(1)).isEqualTo("      [validate] Semantic checks");
                    assertThat(lines.get(2)).isEqualTo("      [validate] Scanned 3 table(s) in 0m01s");
                });
    }
}
