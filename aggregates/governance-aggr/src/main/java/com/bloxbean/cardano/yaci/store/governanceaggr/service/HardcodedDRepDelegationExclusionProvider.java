package com.bloxbean.cardano.yaci.store.governanceaggr.service;

import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.yaci.core.model.governance.DrepType;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.DRepDelegationExclusion;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class HardcodedDRepDelegationExclusionProvider implements DRepDelegationExclusionProvider {

    @Override
    public List<DRepDelegationExclusion> getExclusionsForNetwork(long protocolMagic) {
        if (protocolMagic == Networks.mainnet().getProtocolMagic()) {
            return mainnetExclusions();
        }

        if (protocolMagic == Networks.preprod().getProtocolMagic()) {
            return preprodExclusions();
        }

        if (protocolMagic == Networks.preview().getProtocolMagic()) {
            return previewExclusions();
        }

        return Collections.emptyList();
    }

    private List<DRepDelegationExclusion> mainnetExclusions() {
        return List.of(
                DRepDelegationExclusion.builder()
                        .address("stake1u970ndmm5eu2d8f959tydqjehky3uzkhv8sduapwy6lr5js5lu07c")
                        .drepHash("426d5a97aee15433731560fd5027178f693d2261c24d7c52123f964b")
                        .drepType(DrepType.ADDR_KEYHASH)
                        .slot(134660740L)
                        .txIndex(3)
                        .certIndex(0)
                        .build(),
                DRepDelegationExclusion.builder()
                        .address("stake1uxshln6sur0vcl8t0wl7ew7p3eyu0gvp607592xy2sjtnxcsfu3h3")
                        .drepHash("426d5a97aee15433731560fd5027178f693d2261c24d7c52123f964b")
                        .drepType(DrepType.ADDR_KEYHASH)
                        .slot(134660628L)
                        .txIndex(1)
                        .certIndex(0)
                        .build(),
                DRepDelegationExclusion.builder()
                        .address("stake1uyp93uyq8z3tv9tqaa7l2z5jgzps7ag8m52wezthynx8wyslvlp5v")
                        .drepHash("426d5a97aee15433731560fd5027178f693d2261c24d7c52123f964b")
                        .drepType(DrepType.ADDR_KEYHASH)
                        .slot(134661063L)
                        .txIndex(6)
                        .certIndex(0)
                        .build(),

                DRepDelegationExclusion.builder()
                        .address("stake1uxk2mcvesw4nl8zj5xzgg9dvqqu6f0kjms3v8gxjujktvnqek6jq6")
                        .drepHash("8af81c6e86446b617ae0f609e4ebc154e02febefced49936ad0d2d64")
                        .drepType(DrepType.ADDR_KEYHASH)
                        .slot(134071014L)
                        .txIndex(36)
                        .certIndex(0)
                        .build(),
                DRepDelegationExclusion.builder()
                        .address("stake1uxk2mcvesw4nl8zj5xzgg9dvqqu6f0kjms3v8gxjujktvnqek6jq6")
                        .drepHash("8af81c6e86446b617ae0f609e4ebc154e02febefced49936ad0d2d64")
                        .drepType(DrepType.ADDR_KEYHASH)
                        .slot(134072195L)
                        .txIndex(38)
                        .certIndex(0)
                        .build(),

                DRepDelegationExclusion.builder()
                        .address("stake1u835ecxtx8pf0nshsghc7pqlj2valtrvlqnf8g5jz9u222g8aksvg")
                        .drepHash("a4a01438f9e942e8b9e97101ddabeea1f9f7785a05c670139dbfa4f2")
                        .drepType(DrepType.ADDR_KEYHASH)
                        .slot(139181268L)
                        .txIndex(1)
                        .certIndex(0)
                        .build(),

                DRepDelegationExclusion.builder()
                        .address("stake1uxryzucsv5zczwjf3dsmqd2q67zyxfafajrl3749aqv0xzce4hz5m")
                        .drepHash("d2e0ed6a4b1b3233f071244420293f7eeb077ebf758e74b3876d4552")
                        .drepType(DrepType.ADDR_KEYHASH)
                        .slot(137196813L)
                        .txIndex(21)
                        .certIndex(0)
                        .build(),

                DRepDelegationExclusion.builder()
                        .address("stake1u9zmae2z60csj4k540slxhr0clfgpqa7yzsreav9tm490mc596ce2")
                        .drepHash("9a159303b414847725a7f39beb5b7fdf0665653e2b5bfea3df738f76")
                        .drepType(DrepType.ADDR_KEYHASH)
                        .slot(139783792L)
                        .txIndex(32)
                        .certIndex(0)
                        .build(),

                DRepDelegationExclusion.builder()
                        .address("stake1uyenkwwzynd4hyrcv5nkwdqeqgd6uqdpy3y5fajjkxdqtkcn8dk3r")
                        .drepHash("b102197ee2affaebd50fcb8ca69fb4fa9eba931f4cc219f18db6d7e6")
                        .drepType(DrepType.ADDR_KEYHASH)
                        .slot(141998555L)
                        .txIndex(18)
                        .certIndex(0)
                        .build(),

                DRepDelegationExclusion.builder()
                        .address("stake1uyyy4wnk26t9k9d9rxlxjs6uldzx5pqcw40yy9fcx6n4qgcgxftuz")
                        .drepHash("8b75035882d4165bea8000c4d3f2c123ae33c1d92a751a78135a2402")
                        .drepType(DrepType.ADDR_KEYHASH)
                        .slot(140560780L)
                        .txIndex(11)
                        .certIndex(0)
                        .build(),
                DRepDelegationExclusion.builder()
                        .address("stake1uy63zkhkpgmzt364wdtmv37tzs2me3q4amm090t5wudlz3ck5jkp2")
                        .drepHash("8b75035882d4165bea8000c4d3f2c123ae33c1d92a751a78135a2402")
                        .drepType(DrepType.ADDR_KEYHASH)
                        .slot(145772618L)
                        .txIndex(20)
                        .certIndex(0)
                        .build(),
                DRepDelegationExclusion.builder()
                        .address("stake1uy63zkhkpgmzt364wdtmv37tzs2me3q4amm090t5wudlz3ck5jkp2")
                        .drepHash("8b75035882d4165bea8000c4d3f2c123ae33c1d92a751a78135a2402")
                        .drepType(DrepType.ADDR_KEYHASH)
                        .slot(145771679L)
                        .txIndex(2)
                        .certIndex(0)
                        .build(),
                DRepDelegationExclusion.builder()
                        .address("stake1u98dspy6xclzwdr5ntpp6dqfzh284e6qcvv557r9f3qtuss4szc7u")
                        .drepHash("8b75035882d4165bea8000c4d3f2c123ae33c1d92a751a78135a2402")
                        .drepType(DrepType.ADDR_KEYHASH)
                        .slot(140026000L)
                        .txIndex(2)
                        .certIndex(0)
                        .build(),
                DRepDelegationExclusion.builder()
                        .address("stake1u9wrhx23e0gr5cy8ay9znv24qm0sqjewdkdsz9wzva35yqc7d9lja")
                        .drepHash("8b75035882d4165bea8000c4d3f2c123ae33c1d92a751a78135a2402")
                        .drepType(DrepType.ADDR_KEYHASH)
                        .slot(136236835L)
                        .txIndex(4)
                        .certIndex(0)
                        .build(),
                DRepDelegationExclusion.builder()
                        .address("stake1uxp3yk2k55efksl9ns2wgr0aqustzq0s5ja8uay38wu2ekgqxdt57")
                        .drepHash("8b75035882d4165bea8000c4d3f2c123ae33c1d92a751a78135a2402")
                        .drepType(DrepType.ADDR_KEYHASH)
                        .slot(138211565L)
                        .txIndex(4)
                        .certIndex(0)
                        .build(),
                DRepDelegationExclusion.builder()
                        .address("stake1u88eu4y2uhrq28cy5gldsngern7840u26282vm75pcwq0yslwqvff")
                        .drepHash("8b75035882d4165bea8000c4d3f2c123ae33c1d92a751a78135a2402")
                        .drepType(DrepType.ADDR_KEYHASH)
                        .slot(136514924L)
                        .txIndex(0)
                        .certIndex(0)
                        .build(),
                DRepDelegationExclusion.builder()
                        .address("stake1u8uzysgh40jyp3d5pz2cmzqu4n30uua34njhaywmnm8wsksspt3e9")
                        .drepHash("8b75035882d4165bea8000c4d3f2c123ae33c1d92a751a78135a2402")
                        .drepType(DrepType.ADDR_KEYHASH)
                        .slot(140142764L)
                        .txIndex(9)
                        .certIndex(0)
                        .build(),

                DRepDelegationExclusion.builder()
                        .address("stake1u98tshk6rwtc5pffzjh2nskx6kgmgj83dqq7ahw2a7sqfhsh8tsrm")
                        .drepHash("43d159afe83267d9b160ef6afa1223e4040226e6aea130aecb139073")
                        .drepType(DrepType.ADDR_KEYHASH)
                        .slot(139181430L)
                        .txIndex(2)
                        .certIndex(0)
                        .build(),

                DRepDelegationExclusion.builder()
                        .address("stake1uymmy4adzhu9qesq7hx7y4cdplzy5jjuu0mev6sjdu3y5gclhj5py")
                        .drepHash("0ec943dc5e7c2f308d39322a2d12c934fac5b2926e76c67e84b2c8d1")
                        .drepType(DrepType.ADDR_KEYHASH)
                        .slot(136963166L)
                        .txIndex(17)
                        .certIndex(0)
                        .build(),

                DRepDelegationExclusion.builder()
                        .address("stake1uxxx4sncwj7qltp5lee5gdjkr7p9gp4emnvcwhujn9dj9ys26xe69")
                        .drepHash("b6f4547ad049d7443ee5695761e1aa5e446cfccf72c7ed0d8ad8edfa")
                        .drepType(DrepType.ADDR_KEYHASH)
                        .slot(139181466L)
                        .txIndex(22)
                        .certIndex(0)
                        .build(),

                DRepDelegationExclusion.builder()
                        .address("stake1uy8jyj364kqcu857m6n3aatu5tv3ynhje7ztpek3z8hlvacksknpc")
                        .drepHash("55215f98aa7d9e289d215a55f62d258c6c3a71ab847b76de9ddbe661")
                        .drepType(DrepType.ADDR_KEYHASH)
                        .slot(134757564L)
                        .txIndex(2)
                        .certIndex(0)
                        .build(),

                DRepDelegationExclusion.builder()
                        .address("stake1uxkyk9j6rhx3amsr6vsgh89dwk67yv2qv4ptalgqfmy85pgxq6eu6")
                        .drepHash("a4d8e2b7421eb6f3b438b58d56c51a687f8a32fe25f36249cf601fe8")
                        .drepType(DrepType.ADDR_KEYHASH)
                        .slot(139181055L)
                        .txIndex(13)
                        .certIndex(0)
                        .build(),

                DRepDelegationExclusion.builder()
                        .address("stake1ux8nrx07r8y03dwsqe5mux5r683dc5lek06gq0ks4rqce5c5yke48")
                        .drepHash("0dd84c5b1e5801f4ac86030254c9a8c0bb8ac1d0c7a1092b57f1e9c3")
                        .drepType(DrepType.ADDR_KEYHASH)
                        .slot(135277977L)
                        .txIndex(12)
                        .certIndex(0)
                        .build(),
                DRepDelegationExclusion.builder()
                        .address("stake1u9uzqhh7tps5syx9dhe85fr3epfmzk288ff648zq4cu735c5em0ksstake1u9uzqhh7tps5syx9dhe85fr3epfmzk288ff648zq4cu735c5em0ks")
                        .drepHash("f4b4ee0811b7a6668da741be7cd9e31044926d32ee0919af9f1ad3e9")
                        .drepType(DrepType.ADDR_KEYHASH)
                        .slot(143584927L)
                        .txIndex(7)
                        .certIndex(0)
                        .build(),
                DRepDelegationExclusion.builder()
                        .address("stake1uxr98j3jg9dgxs3jtfnjg20x8nwaf2gsahxlj5vj7ycclzqy5hra2")
                        .drepHash("2358e09cc591f954fdd2038c93c9b2704a1a892a35476f319e4a056d")
                        .drepType(DrepType.ADDR_KEYHASH)
                        .slot(144049770L)
                        .txIndex(1)
                        .certIndex(0)
                        .build()
        );
    }

    private List<DRepDelegationExclusion> preprodExclusions() {
        return Collections.emptyList();
    }

    private List<DRepDelegationExclusion> previewExclusions() {
        return Collections.emptyList();
    }
}

