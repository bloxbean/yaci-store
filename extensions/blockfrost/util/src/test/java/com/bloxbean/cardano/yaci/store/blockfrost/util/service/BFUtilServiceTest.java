package com.bloxbean.cardano.yaci.store.blockfrost.util.service;

import com.bloxbean.cardano.yaci.store.blockfrost.util.dto.BFDeriveAddressDto;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.submit.service.TxEvaluationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class BFUtilServiceTest {
    private static final long MAINNET_PROTOCOL_MAGIC = 764824073L;
    private static final long PREPROD_PROTOCOL_MAGIC = 1097911063L;
    private static final String XPUB = "8e8131277ac46bf6b526fff4fe00cecc71026c278340c44458878bd162f70367505e1ea016e67052b3697ac749e4b034010056e7fd8e8486682665cd9693fb21";

    @Test
    void deriveAddressReturnsTestnetBaseAddress() {
        BFDeriveAddressDto result = service(PREPROD_PROTOCOL_MAGIC).deriveAddress(XPUB, 0, 0);

        assertThat(result.getXpub()).isEqualTo(XPUB);
        assertThat(result.getRole()).isEqualTo(0);
        assertThat(result.getIndex()).isEqualTo(0);
        assertThat(result.getAddress()).isEqualTo("addr_test1qzm3qwm0gsda3vavu43a4fe9ls8xlfjv45mjadxv6z5a3uq5wh8uvn3vr6vd7n80yyluxt87u336syy9678flma06sfs33797j");
    }

    @Test
    void deriveAddressReturnsMainnetBaseAddressWhenProtocolMagicIsMainnet() {
        BFDeriveAddressDto result = service(MAINNET_PROTOCOL_MAGIC).deriveAddress(XPUB, 0, 0);

        assertThat(result.getAddress()).isEqualTo("addr1qxm3qwm0gsda3vavu43a4fe9ls8xlfjv45mjadxv6z5a3uq5wh8uvn3vr6vd7n80yyluxt87u336syy9678flma06sfsj8r9jd");
    }

    @Test
    void deriveAddressUsesRequestedRoleAndIndex() {
        BFDeriveAddressDto result = service(PREPROD_PROTOCOL_MAGIC).deriveAddress(XPUB, 1, 7);

        assertThat(result.getRole()).isEqualTo(1);
        assertThat(result.getIndex()).isEqualTo(7);
        assertThat(result.getAddress()).isEqualTo("addr_test1qz7v5x33xz6dek5ssh25dm80hjp5a566q3f9gml870w2m3c5wh8uvn3vr6vd7n80yyluxt87u336syy9678flma06sfsjceddk");
    }

    @Test
    void deriveAddressRejectsInvalidXpubLength() {
        assertThatThrownBy(() -> service(PREPROD_PROTOCOL_MAGIC).deriveAddress("abcd", 0, 0))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getReason()).isEqualTo("Invalid xpub length. Expected 64 bytes (public key + chain code), got 2");
                });
    }

    private BFUtilService service(long protocolMagic) {
        StoreProperties storeProperties = new StoreProperties();
        storeProperties.setProtocolMagic(protocolMagic);
        return new BFUtilService(mock(TxEvaluationService.class), storeProperties);
    }
}
