package com.bloxbean.cardano.yaci.store.account.storage.impl.mapper;

import javax.annotation.processing.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T15:09:26+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250514-1000, environment: Java 21.0.7 (Eclipse Adoptium)"
)
public class AccountMapperImpl extends AccountMapperDecorator {

    private final AccountMapper delegate;

    public AccountMapperImpl() {
        this( new AccountMapperImpl_() );
    }

    private AccountMapperImpl(AccountMapperImpl_ delegate) {
        super( delegate );
        this.delegate = delegate;
    }
}
