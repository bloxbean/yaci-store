package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "metadata_reference_nft")
@IdClass(MetadataReferenceNftId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetadataReferenceNft {

    @Id
    private String policyId;
    @Id
    private String assetName;
    @Id
    private Long slot;

    private String name;

    private String description;

    private String ticker;

    private String url;

    private Long decimals;

    private String logo;

    private Long version;

    private String datum;

}
