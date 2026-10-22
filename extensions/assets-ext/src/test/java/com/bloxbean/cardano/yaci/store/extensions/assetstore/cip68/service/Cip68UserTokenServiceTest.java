package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.service;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.Cip68UserToken;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.repository.Cip68UserTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Cip68UserTokenService")
class Cip68UserTokenServiceTest {

    private static final String POLICY = "aa".repeat(28);
    private static final String FT = POLICY + "0014df10" + "6162";
    private static final String RFT = POLICY + "001bc280" + "6162";

    private Cip68UserTokenRepository repository;
    private Cip68UserTokenService service;

    @BeforeEach
    void setUp() {
        repository = mock(Cip68UserTokenRepository.class);
        service = new Cip68UserTokenService(repository);
    }

    private static Cip68UserToken token(String unit) {
        return Cip68UserToken.builder().unit(unit).slot(1L).txHash("bb".repeat(32)).build();
    }

    @Test
    @DisplayName("a unit is looked up in lower case, whatever case the request used")
    void lookupIsCaseInsensitive() {
        when(repository.existsById(FT)).thenReturn(true);

        assertThat(service.exists(FT.toUpperCase())).isTrue();
        assertThat(service.exists(RFT)).isFalse();
    }

    @Test
    @DisplayName("existing() answers which of the units were seen, with one query")
    void existingIsOneQuery() {
        when(repository.findAllById(any())).thenReturn(List.of(token(FT)));

        assertThat(service.existing(List.of(FT, RFT.toUpperCase()))).containsExactly(FT);
        verify(repository).findAllById(any());
    }

    @Test
    @DisplayName("existing() of no units does not query")
    void existingOfNothing() {
        assertThat(service.existing(List.of())).isEmpty();
        verify(repository, never()).findAllById(any());
    }

    @Test
    @DisplayName("the policy and name form exists when the 333 or the 444 token was seen")
    void fungibleOrRichFungible() {
        when(repository.findAllById(any())).thenReturn(List.of());
        assertThat(service.existsAsFungibleOrRichFungible(POLICY, "6162")).isFalse();

        when(repository.findAllById(Set.of(FT, RFT))).thenReturn(List.of(token(RFT)));
        assertThat(service.existsAsFungibleOrRichFungible(POLICY, "6162")).isTrue();
    }

    @Test
    @DisplayName("exists() never throws for a unit that is not recorded")
    void unknownUnit() {
        when(repository.existsById(anyString())).thenReturn(false);

        assertThat(service.exists("ff".repeat(10))).isFalse();
    }
}
