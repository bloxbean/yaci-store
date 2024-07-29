package com.bloxbean.cardano.yaci.store.epoch.storage.impl.model;

import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "local_epoch_param")
public class LocalEpochParamsEntity {
    @Id
    private Integer epoch;

    @Type(JsonType.class)
    @Column(name = "params")
    private ProtocolParams protocolParams;

    @UpdateTimestamp
    @Column(name = "update_datetime")
    private LocalDateTime updateDateTime;
}
