package com.bloxbean.cardano.yaci.store.utxo.storage.impl.mapper;

import javax.annotation.processing.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T15:09:25+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250514-1000, environment: Java 21.0.7 (Eclipse Adoptium)"
)
public class UtxoMapperImpl extends UtxoMapperDecorator {

    private final UtxoMapper delegate;

    public UtxoMapperImpl() {
        this( new UtxoMapperImpl_() );
    }

    private UtxoMapperImpl(UtxoMapperImpl_ delegate) {
        super( delegate );
        this.delegate = delegate;
    }
}
