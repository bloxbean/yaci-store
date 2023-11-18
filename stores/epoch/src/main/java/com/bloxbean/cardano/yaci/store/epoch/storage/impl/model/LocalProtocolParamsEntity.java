package com.bloxbean.cardano.yaci.store.epoch.storage.impl.model;

import com.bloxbean.cardano.client.api.model.ProtocolParams;
import com.bloxbean.cardano.yaci.store.common.model.BaseEntity;
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
@Table(name = "local_protocol_params")
public class LocalProtocolParamsEntity extends BaseEntity {
    @Id
    private Long id;

    @Type(JsonType.class)
    @Column(name = "params")
    private ProtocolParams protocolParams;

}
