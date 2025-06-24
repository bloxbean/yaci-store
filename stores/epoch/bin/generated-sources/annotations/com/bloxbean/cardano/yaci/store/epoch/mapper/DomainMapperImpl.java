package com.bloxbean.cardano.yaci.store.epoch.mapper;

import javax.annotation.processing.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T15:09:20+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250514-1000, environment: Java 21.0.7 (Eclipse Adoptium)"
)
public class DomainMapperImpl extends DomainMapperDecorator {

    private final DomainMapper delegate;

    public DomainMapperImpl() {
        this( new DomainMapperImpl_() );
    }

    private DomainMapperImpl(DomainMapperImpl_ delegate) {
        super( delegate );
        this.delegate = delegate;
    }
}
