package com.bloxbean.cardano.yaci.store.script.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class Datum {
    private String hash;
    private String datum;
    private String createdAtTx;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Datum datum = (Datum) o;
        return Objects.equals(hash, datum.hash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hash);
    }
}
