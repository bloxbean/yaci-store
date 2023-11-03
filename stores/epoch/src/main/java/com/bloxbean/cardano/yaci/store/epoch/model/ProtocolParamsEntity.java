package com.bloxbean.cardano.yaci.store.epoch.model;

import com.bloxbean.cardano.yaci.store.common.model.BaseEntity;
import com.bloxbean.cardano.client.api.model.ProtocolParams;
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
@Table(name = "protocol_params")
public class ProtocolParamsEntity extends BaseEntity {
    @Id
    private Long id;

    @Type(JsonType.class)
    @Column(name = "params")
    private ProtocolParams protocolParams;

}
