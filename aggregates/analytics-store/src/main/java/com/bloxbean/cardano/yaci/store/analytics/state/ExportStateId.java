package com.bloxbean.cardano.yaci.store.analytics.state;

import lombok.*;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportStateId implements Serializable {

    @Column(name = "table_name", length = 100)
    private String tableName;

    @Column(name = "partition_value", length = 50)
    private String partitionValue;
}
