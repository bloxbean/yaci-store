package com.bloxbean.cardano.yaci.store.protocolparams.storage.impl.jpa.model;

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

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "epoch_param")
public class EpochParamEntity {
    @Id
    @Column(name = "epoch")
    private Integer epoch;

    @Type(JsonType.class)
    @Column(name = "params", columnDefinition = "json")
    private ProtocolParams params;

    @Column(name = "slot")
    private Long slot;
}
