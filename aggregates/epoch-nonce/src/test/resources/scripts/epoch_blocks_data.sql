-- Test block data from mainnet epoch 519 (3 consecutive blocks with VRF results)
-- Block numbers 11043368, 11043369, 11043370
-- These are Babbage-era blocks using vrfResult (not nonceVrf)

TRUNCATE TABLE block;

INSERT INTO block
(hash, number, body_hash, body_size, epoch, total_output, total_fees, block_time, era, issuer_vkey,
 leader_vrf, nonce_vrf, prev_hash, protocol_version, slot, vrf_result, vrf_vkey, no_of_txs, slot_leader,
 create_datetime, update_datetime, epoch_slot, op_cert_hot_vkey, op_cert_seq_number, op_cert_kes_period, op_cert_sigma)
VALUES ('758ddf8af174ff1fbd399d5304e2cdae52916315fdcdc3fc073c8878c7a8faeb', 11043368,
        '0d7814752986bd0ac5cb09abf4e176ab50c0e092ae66dc4203bab463d5fd5698', 74144, 519, 342195420229, 8290173,
        1730569734, 7,
        'f08638c6224212833a7cf5eaef7ec2d0cd6212b63266e7d64ba1c7d51e27b45a', NULL, NULL,
        '733f018bb60ccaeb45a52af73758eae51a018addea2d80ce5ec80df27a73c50a', '9.1', 139003443,
        '{"proof": "61e5f104b665f6aa9deb6682a75112c6f6a14af9340eff940f86e55b99c7e8e907e3361525258ff81e964f4c41ca45af4e0c3f942345fdd00356de4b54e097b0eab39234edaabf9c7f5c6b4e1f5b5c05", "output": "c18a5fa01c9149d984fec409ad7e14ad99ef9d2f2d1ec83dd8cf29c93cef61c796015f121b8f82ba6e6ad841ac3316cccf291b9716ad6f712778a8b3aafc269d"}' FORMAT JSON,
        '9fdc95b6a6cf2e88b137ec6eba9239fc123b952a205f6af09481a9fe04684d28', 17,
        '8dcdf33740ee8e9da6e36337d875fb9222f5c8a1a315fda36886c615', '2024-11-02 22:08:54.000',
        '2024-11-02 22:08:54.000', 158643, '6e96dea84af9b06b51c950f25ce73d3d99b2a439b26628a6217bea32e33a9d5b', 12, 1063,
        '0c548240a94919e256a23b8859cfe275bcc7cebb60ace4c3a34f5b59b7c233987158430563593cabe9c5efc41bd9fb2cbd2aa477842424a845451f190a9e9507');

INSERT INTO block
(hash, number, body_hash, body_size, epoch, total_output, total_fees, block_time, era, issuer_vkey,
 leader_vrf, nonce_vrf, prev_hash, protocol_version, slot, vrf_result, vrf_vkey, no_of_txs, slot_leader,
 create_datetime, update_datetime, epoch_slot, op_cert_hot_vkey, op_cert_seq_number, op_cert_kes_period, op_cert_sigma)
VALUES ('0c37ee61d1d71b2896c4427d95fa8b8dff02c6b72c8c225b64aced830865c0c6', 11043369,
        '3db0af6f24ab966bbef13609f33756ee76d0a1b8bab3862a30253d9955fdffd2', 22069, 519, 191245720452, 6651387,
        1730569751, 7,
        '61baeaac8e9a5e81b55346f2a38f31fef87cf6c604a5d8f0083d8bc3f197cc09', NULL, NULL,
        '758ddf8af174ff1fbd399d5304e2cdae52916315fdcdc3fc073c8878c7a8faeb', '9.1', 139003460,
        '{"proof": "8d6092717bb9681cd885ae124386bd69b667509b0c6f4ae54362f04e94626edbce832959cfe2c39abfc826037651b743ab7a2961efc2be8c0472a564c15fe1bf70e8d82138902b27b36b4cf0e3789406", "output": "ff9af59c9b0bd179b38a112565ee94dc4972c0b773099a6eb1121f0981a85cd34f120caa0080e3a652ce6c6b1944a9709d908a7d0e92a2a9823ec6dadcde4b0f"}' FORMAT JSON,
        '6a2f21dba1a71724e858314ace195cef18cc9023ca6af797d3fd30e3cbcb4ac9', 19,
        '446109cdad3e7dfc6e0bb4be5ecf25df02fcc2ec8f6df46a3ca2c537', '2024-11-02 22:09:11.000',
        '2024-11-02 22:09:11.000', 158660, 'adcde6d45f551e87a9fd4e9c5d4818e20121a3a03b10cd56ed969864e22c4f36', 16, 1050,
        '32a73241163f504d01845a6fad4b5c3182afc0d35cc68de08a3139d1b1eb2732905c0c74d75e841f5bcbf661297d89edb7c24b60bb132c85bc966d98d258940a');

INSERT INTO block
(hash, number, body_hash, body_size, epoch, total_output, total_fees, block_time, era, issuer_vkey,
 leader_vrf, nonce_vrf, prev_hash, protocol_version, slot, vrf_result, vrf_vkey, no_of_txs, slot_leader,
 create_datetime, update_datetime, epoch_slot, op_cert_hot_vkey, op_cert_seq_number, op_cert_kes_period, op_cert_sigma)
VALUES ('1133de13b71ca64825437e212d1d114d31816ce9ce61a5301f93ee6806aa496c', 11043370,
        '3555b0c12808ed531be7f214cde70d3b2f26d297620524630dce79de4abcf6aa', 56066, 519, 2433505959258, 10472556,
        1730569786, 7,
        'ae4e2939e63baff4c26d87cd41a52a27dad64210cd9d509291af4936b9df541c', NULL, NULL,
        '0c37ee61d1d71b2896c4427d95fa8b8dff02c6b72c8c225b64aced830865c0c6', '9.1', 139003495,
        '{"proof": "b0fee663d262f16937d2cd98e6565bafd7de10c9da900ff28c4be4cb4926b3e7bb5e1e57f6f23b1e1bf19d7d7d922bc7355ebc511528ff8e15e668b37875fa11928fed9a215608a55aee3017f4045804", "output": "720121d750ea77c65e393c78012df784ec79231ab7b81c8eb7c31b3ca393367f0ce8f020a727156e05ecf8c9cd52c3bbbd3309832cd32fd53379a1f420ea6bc9"}' FORMAT JSON,
        '31cb5adb52a794e0b562aae71f943dda9adb875d9c69bed7882ec6a1b009f8a3', 24,
        '9679eaa0fa242a9cdae4b030e714b66c0119fc9b3f7564b8f03a5316', '2024-11-02 22:09:46.000',
        '2024-11-02 22:09:46.000', 158695, '860c8edc025799c1eaa522316a56afea3fdf5169d2b99d074584104e7ec9ce63', 20, 1046,
        '855c6019c081cd285d28d397e668d113b1329631d358e9a19414b3ac297ddbde93b3c11593601a5382fb87b8392df579408ffd957cf826871327968278011b02');
