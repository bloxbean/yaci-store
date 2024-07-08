package com.bloxbean.cardano.yaci.store.adapot.service;

import org.springframework.stereotype.Component;

import java.util.Set;

import static java.util.Collections.EMPTY_SET;

@Component
public class SharedPoolRewardAddresses {

    public Set<String> getSharedPoolRewardAddressesWithoutReward(int epoch) {
        if (epoch == 214) {
            return Set.of("pool13l0j202yexqh6l0awtee9g354244gmfze09utxz0sn7p7r3ev3m",
                    "pool166dkk9kx5y6ug9tnvh0dnvxhwt2yca3g5pd5jaqa8t39cgyqqlr");
        } else if(epoch == 215) {
            return Set.of(
                    "pool17rns3wjyql9jg9xkzw9h88f0kstd693pm6urwxmvejqgsyjw7ta",
                    "pool166dkk9kx5y6ug9tnvh0dnvxhwt2yca3g5pd5jaqa8t39cgyqqlr"
            );
        } else if (epoch == 216) {
            return Set.of(
                    "pool13l0j202yexqh6l0awtee9g354244gmfze09utxz0sn7p7r3ev3m",
                    "pool166dkk9kx5y6ug9tnvh0dnvxhwt2yca3g5pd5jaqa8t39cgyqqlr"
            );
        } else if (epoch == 218) {
            return Set.of(
                    "pool13l0j202yexqh6l0awtee9g354244gmfze09utxz0sn7p7r3ev3m",
                    "pool17rns3wjyql9jg9xkzw9h88f0kstd693pm6urwxmvejqgsyjw7ta"
            );
        } else if (epoch == 219) {
            return Set.of(
                    "pool13l0j202yexqh6l0awtee9g354244gmfze09utxz0sn7p7r3ev3m",
                    "pool166dkk9kx5y6ug9tnvh0dnvxhwt2yca3g5pd5jaqa8t39cgyqqlr"
            );
        } else if (epoch == 220) {
            return Set.of(
                    "pool13l0j202yexqh6l0awtee9g354244gmfze09utxz0sn7p7r3ev3m"
            );
        } else
            return EMPTY_SET;
    }
}
