package com.bloxbean.cardano.yaci.store.staking.util;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.address.AddressType;
import com.bloxbean.cardano.client.common.model.Network;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.exception.AddressRuntimeException;
import com.bloxbean.cardano.client.transaction.spec.NetworkId;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.core.model.certs.StakeCredential;
import lombok.NonNull;

import static com.bloxbean.cardano.client.address.util.AddressEncoderDecoderUtil.*;

//TODO -- Remove this class once the cardano-client-lib is updated to 0.5.0
public class AddressUtil {
    private static final byte RWD_STAKE_KEY_HEDER_TYPE = (byte)0b1110_0000;
    private static final byte RWD_STAKE_SCRIPT_HEADER_TYPE = (byte)0b1111_0000;

    public static Address getRewardAddress(@NonNull StakeCredential stakeCredential, boolean isMainnet) {
        Network network = isMainnet ? Networks.mainnet() : Networks.testnet();

        switch (stakeCredential.getType()) {
            case ADDR_KEYHASH:
                return getAddress(null, HexUtil.decodeHexString(stakeCredential.getHash()),
                        RWD_STAKE_KEY_HEDER_TYPE, network, AddressType.Reward);
            case SCRIPTHASH:
                return getAddress(null, HexUtil.decodeHexString(stakeCredential.getHash()),
                        RWD_STAKE_SCRIPT_HEADER_TYPE, network, AddressType.Reward);
            default:
                throw new AddressRuntimeException("Invalid credential type, should be either Key or Script. Stake Credential: "
                        + stakeCredential);
        }
    }

    private static Address getAddress(byte[] paymentKeyHash, byte[] stakeKeyHash, byte headerKind, Network networkInfo, AddressType addressType) {
        NetworkId network = getNetworkId(networkInfo);

        //get prefix
        String prefix = getPrefixHeader(addressType) + getPrefixTail(network);

        //get header
        byte header = getAddressHeader(headerKind, networkInfo, addressType);
        byte[] addressArray = getAddressBytes(paymentKeyHash, stakeKeyHash, addressType, header);

        return new Address(prefix, addressArray);
    }

    private static byte[] getAddressBytes(byte[] paymentKeyHash, byte[] stakeKeyHash, AddressType addressType, byte header) {
        //get body
        byte[] addressArray;
        switch (addressType) {
            case Base:
                addressArray = new byte[1 + paymentKeyHash.length + stakeKeyHash.length];
                addressArray[0] = header;
                System.arraycopy(paymentKeyHash, 0, addressArray, 1, paymentKeyHash.length);
                System.arraycopy(stakeKeyHash, 0, addressArray, paymentKeyHash.length + 1, stakeKeyHash.length);
                break;
            case Enterprise:
                addressArray = new byte[1 + paymentKeyHash.length];
                addressArray[0] = header;
                System.arraycopy(paymentKeyHash, 0, addressArray, 1, paymentKeyHash.length);
                break;
            case Reward:
                addressArray = new byte[1 + stakeKeyHash.length];
                addressArray[0] = header;
                System.arraycopy(stakeKeyHash, 0, addressArray, 1, stakeKeyHash.length);
                break;
            case Ptr:
                addressArray = new byte[1 + paymentKeyHash.length + stakeKeyHash.length];
                addressArray[0] = header;
                System.arraycopy(paymentKeyHash, 0, addressArray, 1, paymentKeyHash.length);
                System.arraycopy(stakeKeyHash, 0, addressArray, paymentKeyHash.length + 1, stakeKeyHash.length);
                break;
            default:
                throw new AddressRuntimeException("Unknown address type");
        }
        return addressArray;
    }
}
