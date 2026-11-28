package com.bloxbean.cardano.yaci.store.admin.cli.snapshot;

import com.bloxbean.cardano.yaci.store.snapshot.util.Progress;

import java.util.function.Consumer;

import static com.bloxbean.cardano.yaci.store.admin.cli.common.ConsoleWriter.info;
import static com.bloxbean.cardano.yaci.store.admin.cli.common.ConsoleWriter.writeLn;

/**
 * Prints a snapshot command's progress: numbered phase banners ({@code [2/6] Loading ...}) with the
 * status lines ({@code [load] ...}) indented under them.
 *
 * <p>Untagged per-item events are dropped: every long step reports on a timer, so sampling every
 * Nth event adds nothing but noise.
 */
final class ProgressPrinter implements Consumer<String> {

    private final int phases;
    private int phase;

    /** @param phases the banners to number; 0 when the command sends none */
    ProgressPrinter(int phases) {
        this.phases = phases;
    }

    @Override
    public synchronized void accept(String event) {
        String line = format(event);
        if (line != null) {
            writeLn(line);
        }
    }

    /** The line to print for {@code event}, or null when it is not printed. */
    synchronized String format(String event) {
        if (event.startsWith(Progress.PHASE_PREFIX)) {
            return info("[%d/%d] %s", ++phase, phases, event.substring(Progress.PHASE_PREFIX.length()));
        }
        if (event.startsWith("[")) {
            return "      " + event;
        }
        return null;
    }

    /**
     * A view for a step that runs as one phase of this command and sends banners of its own, such as
     * validation at the end of an import: its banners become {@code [tag]} status lines.
     */
    Consumer<String> nested(String tag) {
        return event -> accept(event.startsWith(Progress.PHASE_PREFIX)
                ? "[" + tag + "] " + event.substring(Progress.PHASE_PREFIX.length()) : event);
    }
}
