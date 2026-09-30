package com.bloxbean.cardano.yaci.store.snapshot.validate;

import java.util.ArrayList;
import java.util.List;

/** Result of one validation level. */
public record ValidationReport(String level, List<Check> checks) {

    public record Check(String name, boolean passed, String detail) {}

    public boolean passed() {
        return checks.stream().allMatch(Check::passed);
    }

    public List<Check> failures() {
        return checks.stream().filter(c -> !c.passed()).toList();
    }

    public static class Builder {
        private final String level;
        private final List<Check> checks = new ArrayList<>();

        public Builder(String level) {
            this.level = level;
        }

        public Builder pass(String name, String detail) {
            checks.add(new Check(name, true, detail));
            return this;
        }

        public Builder fail(String name, String detail) {
            checks.add(new Check(name, false, detail));
            return this;
        }

        public Builder check(String name, boolean ok, String detail) {
            checks.add(new Check(name, ok, detail));
            return this;
        }

        public ValidationReport build() {
            return new ValidationReport(level, List.copyOf(checks));
        }
    }
}
