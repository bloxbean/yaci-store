package com.bloxbean.cardano.yaci.store.adapot.service;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.EMPTY_SET;

@Component
public class SharedPoolRewardAddresses {

    public Set<String> getSharedPoolRewardAddressesWithoutReward(int epoch) {
        Map<Integer, Set<String>> sharedPoolRewardAddresses = new HashMap<>();
        sharedPoolRewardAddresses.put(214, Set.of(
                "pool13l0j202yexqh6l0awtee9g354244gmfze09utxz0sn7p7r3ev3m",
                "pool166dkk9kx5y6ug9tnvh0dnvxhwt2yca3g5pd5jaqa8t39cgyqqlr"
        ));
        sharedPoolRewardAddresses.put(215, Set.of(
                "pool17rns3wjyql9jg9xkzw9h88f0kstd693pm6urwxmvejqgsyjw7ta",
                "pool166dkk9kx5y6ug9tnvh0dnvxhwt2yca3g5pd5jaqa8t39cgyqqlr"
        ));
        sharedPoolRewardAddresses.put(216, Set.of(
                "pool13l0j202yexqh6l0awtee9g354244gmfze09utxz0sn7p7r3ev3m",
                "pool166dkk9kx5y6ug9tnvh0dnvxhwt2yca3g5pd5jaqa8t39cgyqqlr"
        ));

        sharedPoolRewardAddresses.put(218, Set.of(
                "pool13l0j202yexqh6l0awtee9g354244gmfze09utxz0sn7p7r3ev3m",
                "pool17rns3wjyql9jg9xkzw9h88f0kstd693pm6urwxmvejqgsyjw7ta"
        ));
        sharedPoolRewardAddresses.put(219, Set.of(
                "pool13l0j202yexqh6l0awtee9g354244gmfze09utxz0sn7p7r3ev3m",
                "pool166dkk9kx5y6ug9tnvh0dnvxhwt2yca3g5pd5jaqa8t39cgyqqlr"
        ));
        sharedPoolRewardAddresses.put(220, Set.of("pool13l0j202yexqh6l0awtee9g354244gmfze09utxz0sn7p7r3ev3m"));
        sharedPoolRewardAddresses.put(221, Set.of(
                "pool13l0j202yexqh6l0awtee9g354244gmfze09utxz0sn7p7r3ev3m",
                "pool166dkk9kx5y6ug9tnvh0dnvxhwt2yca3g5pd5jaqa8t39cgyqqlr"
        ));
        sharedPoolRewardAddresses.put(222, Set.of("pool150n38x8gquu4yt5s0s6m3ajjqe89y68x96dh32tq8a4es2l6dvp",
                "pool166dkk9kx5y6ug9tnvh0dnvxhwt2yca3g5pd5jaqa8t39cgyqqlr",
                "pool19066qvd5dv6vq7fh5a5l7muzk6nc5fw8zq3w4tclyrhvjvlyeuc",
                "pool1xt7mjrtnsew3v33lu8sf93upf20sxhmcrfnpm82ra46yxk7uy45"
        ));
        sharedPoolRewardAddresses.put(223, Set.of(
                "pool150n38x8gquu4yt5s0s6m3ajjqe89y68x96dh32tq8a4es2l6dvp",
                "pool19066qvd5dv6vq7fh5a5l7muzk6nc5fw8zq3w4tclyrhvjvlyeuc",
                "pool1xt7mjrtnsew3v33lu8sf93upf20sxhmcrfnpm82ra46yxk7uy45"
        ));
        sharedPoolRewardAddresses.put(224, Set.of(
                "pool1xt7mjrtnsew3v33lu8sf93upf20sxhmcrfnpm82ra46yxk7uy45",
                "pool150n38x8gquu4yt5s0s6m3ajjqe89y68x96dh32tq8a4es2l6dvp"
        ));
        sharedPoolRewardAddresses.put(225, Set.of(
                "pool150n38x8gquu4yt5s0s6m3ajjqe89y68x96dh32tq8a4es2l6dvp",
                "pool166dkk9kx5y6ug9tnvh0dnvxhwt2yca3g5pd5jaqa8t39cgyqqlr",
                "pool1xt7mjrtnsew3v33lu8sf93upf20sxhmcrfnpm82ra46yxk7uy45"
        ));
        sharedPoolRewardAddresses.put(226, Set.of(
                "pool150n38x8gquu4yt5s0s6m3ajjqe89y68x96dh32tq8a4es2l6dvp",
                "pool1xt7mjrtnsew3v33lu8sf93upf20sxhmcrfnpm82ra46yxk7uy45"
        ));
        sharedPoolRewardAddresses.put(227, Set.of(
                "pool150n38x8gquu4yt5s0s6m3ajjqe89y68x96dh32tq8a4es2l6dvp",
                "pool1xt7mjrtnsew3v33lu8sf93upf20sxhmcrfnpm82ra46yxk7uy45"
        ));

        sharedPoolRewardAddresses.put(228, Set.of(
                "pool13l0j202yexqh6l0awtee9g354244gmfze09utxz0sn7p7r3ev3m",
                "pool1xt7mjrtnsew3v33lu8sf93upf20sxhmcrfnpm82ra46yxk7uy45",
                "pool150n38x8gquu4yt5s0s6m3ajjqe89y68x96dh32tq8a4es2l6dvp"
        ));

        sharedPoolRewardAddresses.put(229, Set.of());
        sharedPoolRewardAddresses.put(230, Set.of(
                "pool166dkk9kx5y6ug9tnvh0dnvxhwt2yca3g5pd5jaqa8t39cgyqqlr"
        ));

        sharedPoolRewardAddresses.put(231, Set.of());
        sharedPoolRewardAddresses.put(232, Set.of(
                "pool19w5khsnmu27au0kprw0kjm8jr7knneysj7lfkqvnu66hyz0jxsx",
                "pool166dkk9kx5y6ug9tnvh0dnvxhwt2yca3g5pd5jaqa8t39cgyqqlr"
        ));

        sharedPoolRewardAddresses.put(233, Set.of("pool19w5khsnmu27au0kprw0kjm8jr7knneysj7lfkqvnu66hyz0jxsx"));
        sharedPoolRewardAddresses.put(234, Set.of(
                "pool19w5khsnmu27au0kprw0kjm8jr7knneysj7lfkqvnu66hyz0jxsx",
                "pool166dkk9kx5y6ug9tnvh0dnvxhwt2yca3g5pd5jaqa8t39cgyqqlr"
        ));

        sharedPoolRewardAddresses.put(235, Set.of(
                "pool19w5khsnmu27au0kprw0kjm8jr7knneysj7lfkqvnu66hyz0jxsx",
                "pool166dkk9kx5y6ug9tnvh0dnvxhwt2yca3g5pd5jaqa8t39cgyqqlr"
        ));

        sharedPoolRewardAddresses.put(236, Set.of(
                "pool19w5khsnmu27au0kprw0kjm8jr7knneysj7lfkqvnu66hyz0jxsx",
                "pool166dkk9kx5y6ug9tnvh0dnvxhwt2yca3g5pd5jaqa8t39cgyqqlr"
        ));

        sharedPoolRewardAddresses.put(237, Set.of(
                "pool166dkk9kx5y6ug9tnvh0dnvxhwt2yca3g5pd5jaqa8t39cgyqqlr"
        ));

        return sharedPoolRewardAddresses.getOrDefault(epoch, EMPTY_SET);

//        if (epoch == 214) {
//            return Set.of("pool13l0j202yexqh6l0awtee9g354244gmfze09utxz0sn7p7r3ev3m",
//                    "pool166dkk9kx5y6ug9tnvh0dnvxhwt2yca3g5pd5jaqa8t39cgyqqlr");
//        } else if(epoch == 215) {
//            return Set.of(
//                    "pool17rns3wjyql9jg9xkzw9h88f0kstd693pm6urwxmvejqgsyjw7ta",
//                    "pool166dkk9kx5y6ug9tnvh0dnvxhwt2yca3g5pd5jaqa8t39cgyqqlr"
//            );
//        } else if (epoch == 216) {
//            return Set.of(
//                    "pool13l0j202yexqh6l0awtee9g354244gmfze09utxz0sn7p7r3ev3m",
//                    "pool166dkk9kx5y6ug9tnvh0dnvxhwt2yca3g5pd5jaqa8t39cgyqqlr"
//            );
//        } else if (epoch == 218) {
//            return Set.of(
//                    "pool13l0j202yexqh6l0awtee9g354244gmfze09utxz0sn7p7r3ev3m",
//                    "pool17rns3wjyql9jg9xkzw9h88f0kstd693pm6urwxmvejqgsyjw7ta"
//            );
//        } else if (epoch == 219) {
//            return Set.of(
//                    "pool13l0j202yexqh6l0awtee9g354244gmfze09utxz0sn7p7r3ev3m",
//                    "pool166dkk9kx5y6ug9tnvh0dnvxhwt2yca3g5pd5jaqa8t39cgyqqlr"
//            );
//        } else if (epoch == 220) {
//            return Set.of(
//                    "pool13l0j202yexqh6l0awtee9g354244gmfze09utxz0sn7p7r3ev3m"
//            );
//        } else if (epoch == 221) {
//            return Set.of(
//
//            );
//        }  else if (epoch == 222) {
//            return Set.of(
//
//            );
//        } else if (epoch == 223) {
//            return Set.of(
//
//            );
//        } else if (epoch == 224) {
//            return Set.of(
//
//            );
//        } else if (epoch == 225) {
//            return Set.of(
//
//            );
//        } else if (epoch == 226) {
//            return Set.of(
//
//            );
//        } else if (epoch == 227) {
//            return Set.of(
//
//            );
//        } else if (epoch == 228) {
//            return Set.of(
//
//            );
//        } else if (epoch == 229) {
//            return Set.of(
//
//            );
//        } else if (epoch == 230) {
//            return Set.of(
//
//            );
//        } else if (epoch == 231) {
//            return Set.of(
//
//            );
//        } else if (epoch == 232) {
//            return Set.of(
//
//            );
//        } else if (epoch == 233) {
//            return Set.of(
//
//            );
//        } else if (epoch == 234) {
//            return Set.of(
//
//            );
//        } else if (epoch == 235) {
//            return Set.of(
//
//            );
//        } else if (epoch == 236) {
//            return Set.of(
//
//            );
//        }
//        else
//            return EMPTY_SET;
    }
}
