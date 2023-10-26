package com.bloxbean.cardano.yaci.store.staking.util;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.yaci.core.model.certs.StakeCredType;
import com.bloxbean.cardano.yaci.core.model.certs.StakeCredential;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// TODO: move to cardano-client-lib
class AddressUtilTest {

    @Test
    void getRewardAddress_Preprod_1079480() {
        // block 1079480
        String expected = "stake_test1upprgdf9umls0ex79gfj3ymvjeqxuse07z4wk7jutfdxejgzkmsfe";
        Address address = AddressUtil.getRewardAddress(
                new StakeCredential(StakeCredType.ADDR_KEYHASH, "42343525e6ff07e4de2a1328936c96406e432ff0aaeb7a5c5a5a6cc9")
                ,false
                );
        assertThat(address.toBech32()).isEqualTo(expected);
    }

    @Test
    void getRewardAddress_Preprod_1081169() {
        // block 1081169
        String expected = "stake_test17rs4wd04fx4ls9rpdelrp9qwnh3w6cexlmgj42h5t0tvv8gjrqqjc";
        Address address = AddressUtil.getRewardAddress(
                new StakeCredential(StakeCredType.SCRIPTHASH, "e15735f549abf814616e7e30940e9de2ed6326fed12aaaf45bd6c61d")
                ,false
        );
        assertThat(address.toBech32()).isEqualTo(expected);
    }

    @Test
    void getRewardAddress_Mainnet_6771682() {
        // block 6771682
        String expected = "stake1u9mghqk2hfqjgvx3rh00dvuuhkj6ru4yw5vwsrtmx402grcwrthf6";
        Address address = AddressUtil.getRewardAddress(
                new StakeCredential(StakeCredType.ADDR_KEYHASH, "768b82caba412430d11ddef6b39cbda5a1f2a47518e80d7b355ea40f")
                ,true
        );
        assertThat(address.toBech32()).isEqualTo(expected);
    }

    @Test
    void getRewardAddress_Mainnet_8346772() {
        // block 8346772
        String expected = "stake179grhun9hy5a32jw5xxnxpavqkneacsfuh48lgje08atceqkm3wap";
        Address address = AddressUtil.getRewardAddress(
                new StakeCredential(StakeCredType.SCRIPTHASH, "503bf265b929d8aa4ea18d3307ac05a79ee209e5ea7fa25979fabc64")
                ,true
        );
        assertThat(address.toBech32()).isEqualTo(expected);
    }
}
