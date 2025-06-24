TRUNCATE TABLE pool_registration;

INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('ec46ebbe69288a007df2a9fd1dc173cfe5fd679e00c8ed9f652ac2c190d1ceb5', 0, 0,
        '74f5dd2551c2c0fd71aebb95e1f58d0b742c0fd2ff1712644f709bdc',
        '7cc0d9a8931e434c5632d5a696a2c50483d7ed86f4f2e97e558f14e863a2ccbf', 11111000000, 777000000, 1, 5,
        'e0a6f3c57b3e463eb6cffb842b65197356e96b7cb59590a86f8a0b23f9',
        '["a6f3c57b3e463eb6cffb842b65197356e96b7cb59590a86f8a0b23f9"]' FORMAT JSON,
        '[{"ipv4": null, "ipv6": null, "port": 3001, "dnsName": "preprod-relay1.angelstakepool.net"}]' FORMAT JSON,
        'https://angelstakepool.net/preprod/angel.json',
        'bf44709dd714742688eeff2b6ca5573fe312a2e5f49d564c4c2311923c63952c', 28, 10617719,
        '2f449171f3e507edf0425882dc562cc4d29795a967588e913b78891c1b28360a', 174946, 1666300919,
        '2023-10-24 10:07:27.048');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('022972136e2031c25258033369875ba3db342b87af8575b18facf5f1b8a2f3ee', 0, 0,
        '632dc1a4d7ec972aa7c63ba56ce57e1a0fdd8a356f0949f0ded0723a',
        'cf61fc2de833d55d6f16db3fa0ae28936b775976378de49844f35726a66ab7e7', 5000000000, 500000000, 18446744073709551615, 18446744073709551615,
        'e0edd33bac0863e93cf1c33a9713730ec1ba3bfab8f35cb0a50e08b8af',
        '["edd33bac0863e93cf1c33a9713730ec1ba3bfab8f35cb0a50e08b8af"]' FORMAT JSON,
        '[{"ipv4": null, "ipv6": null, "port": 4002, "dnsName": "preprod.canadastakes.ca"}]' FORMAT JSON,
        'https://www.canadastakes.ca/metadata/can1-preprod-metadata.json',
        '7be169056f2a272c90451cab50394c3154fd72450aab3e190056b522792fcc1c', 28, 10618258,
        '3f5e31262469dee43a5b413ea3be961a77d3ba63f332a588093f17675fe8e117', 174974, 1666301458,
        '2023-10-24 10:07:27.255');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('1ad3f62beb20e9bd85d79144bfa5ff4fe2826194278d3a006010528f50f5f6a9', 0, 0,
        '097ed33e2485fe2955bfbffa66d0b3e58909363bfb707c7194ee4e5f',
        '7beebed68148073d78d08c3a9c63ae3e3f9dd656e6744766f64e6ede964f5fa0', 15000000000, 777000000, 18446744073709551610, 18446744073709551615,
        'e06357f7942c6d6018eb6b1c84475e6b08d360ffe9b97e48ce7381e3c1',
        '["6357f7942c6d6018eb6b1c84475e6b08d360ffe9b97e48ce7381e3c1"]' FORMAT JSON,
        '[{"ipv4": null, "ipv6": null, "port": 3001, "dnsName": "preprod-relay1.angelstakepool.net"}]' FORMAT JSON,
        'https://angelstakepool.net/preprod/sion.json',
        '590a8a2ea54c80b216ba5f2250bba03b9d2913f2982999c2510fec307c36035f', 28, 10618552,
        '09362759413c7a6f303f3402a04af8afc7596f5b8cd058d2678adb89a499b99a', 174993, 1666301752,
        '2023-10-24 10:07:27.390');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('a7412e82b9d556162230faa0090535b499160c45766ea429e53e829ed8fe0c04', 0, 0,
        'd84ba9070a6afa3e09f8e8b7f95df16e58f2e8b8a8c1170f579e2a39',
        'c6962c024307b4b9a46a4cced35cffc0c1e32fc138559ec0daf585f821a73b92', 0, 340000000, 1, 10,
        'e04dcca876aac2fcc561f7df3da772d747e2148c9a05c7b27e49a05ea2',
        '["4dcca876aac2fcc561f7df3da772d747e2148c9a05c7b27e49a05ea2"]' FORMAT JSON,
        '[{"ipv4": null, "ipv6": null, "port": 3012, "dnsName": "preprod-r1.panl.org"}]' FORMAT JSON,
        'https://preprod-metadata.panl.org', '0a815d9d2ae73ad7ab76bc69ed5645a099854aeaf295fa4f5863e1f1efe906a0', 28,
        10621472, '51b1ad2df7389ad7e04953ff2d4c16763ee1cb10e8df520f4c5086826ebfc37b', 175137, 1666304672,
        '2023-10-24 10:07:28.409');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('fa4f9a00aee593f13c888d97561705a54acc3e1da052bb3eff7bdef90b936038', 0, 0,
        '64d60fc4972085b884f5d6db08c67be08517c873a692c935b1bdf85e',
        '58dfa1a9929ad7169f1f5edbfd2697436e9e479288ab68d273ec9429b34e0360', 9000000000, 340000000, 2,3,
        'e05b6677d5dbba6f9d6c10fc653d46affd214ba96c98a3c97820caedc9',
        '["5b6677d5dbba6f9d6c10fc653d46affd214ba96c98a3c97820caedc9"]' FORMAT JSON,
        '[{"ipv4": null, "ipv6": null, "port": 6000, "dnsName": "flightrelay1.intertreecryptoconsultants.com"}]' FORMAT JSON,
        'https://tinyurl.com/intrtpreprod', 'a5c79abc117265ae6564e1a4b16103a5cc536d9f558162b1075be6c688963eb8', 28,
        10624838, '702249161d2914143153d1e2fc535f4ba1123cacda76ed6564c559b550aabbff', 175293, 1666308038,
        '2023-10-24 10:07:29.685');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('34c6ff27230dd58c0e647e8b21855b8e022d2cd18c71ea54f2252ee2a39a8eac', 0, 0,
        'ca128569cf8101f21418d52e28c50062803112dd3ad3ccc71885a38f',
        '9ab7a692a1564420e2033122f2a9642247d81033b81c5aff123815803d296797', 8000000000, 340000000, 3, 445,
        'e081ede074dee4a964004d33d319b58344c808136ab4c98a9364dfbc74',
        '["81ede074dee4a964004d33d319b58344c808136ab4c98a9364dfbc74"]' FORMAT JSON,
        '[{"ipv4": null, "ipv6": null, "port": 6000, "dnsName": "relaynode15t.irfada.co"}, {"ipv4": null, "ipv6": null, "port": 6000, "dnsName": "relaynode16t.irfada.co"}]' FORMAT JSON,
        'https://tinyurl.com/4tukbabt', 'e031fc005d9bccaa1cb60e698a832f8ab4b42feb87c791a99d13ad4af4a5958f', 28,
        10634943, 'e24cbe207b093901341b45240d7c56838030268b02b6b986ccb8a168cb67b075', 175795, 1666318143,
        '2023-10-24 10:07:32.742');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('f6e95aa570d79360581d9632be5d0a60efc18c7e1f202bebaea9cf9adc38640e', 0, 0,
        '7facad662e180ce45e5c504957cd1341940c72a708728f7ecfc6e349',
        '96c5e6a9fb8315143834eb2e446c06c18d39b61f284c1abf049ae202e19b9992', 100000000000, 340000000, 9999, 2003999,
        'e04bec2d4e59e40ffc8ac4ed7e3b1e5b32cdc821f5f4b8447cc06d021f',
        '["4bec2d4e59e40ffc8ac4ed7e3b1e5b32cdc821f5f4b8447cc06d021f"]' FORMAT JSON,
        '[{"ipv4": null, "ipv6": null, "port": 4001, "dnsName": "pp-relay1.apexpool.info"}]' FORMAT JSON,
        'https://apexpool.info/preprod/poolmetadata.json',
        'a207f23a6255eeb0b508945e5170b169eb5c3220109d7fa17a7d88d462c8a199', 28, 10646063,
        '13578fb8c230b12d0bd7aeea65e35dd61321dd3669ece3e92b0671db2ffd97aa', 176335, 1666329263,
        '2023-10-24 10:07:35.935');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('7983de4b64bc67ae6d57919bb4dd672b3838912020d98a038cf7b3367a464f71', 0, 0,
        '60015bb5c3bd6aae17d954afab54dc8621135360948733b7b28e89e4',
        '9326291cc62f3b2a422482222c5ed994c3419846b5fde0a66e62b6aaadc4605c', 9000000000, 340000000, 4,6,
        'e06a44bd8ec885d095569aa6a0d69302ff738cfbe8558ef8bba93653b1',
        '["6a44bd8ec885d095569aa6a0d69302ff738cfbe8558ef8bba93653b1"]' FORMAT JSON,
        '[{"ipv4": "89.58.43.194", "ipv6": null, "port": 26000, "dnsName": null}]' FORMAT JSON, 'https://bit.ly/rdlrtpool',
        '8dab596729bfe551a94f6e4bac3da97f4460439f7524c33a1d735e4f6bf8a139', 28, 10647356,
        'e3acf5ab01995bf0f6a28a655d774e9696cd6e2b07521a9b89fe96415c87b453', 176396, 1666330556,
        '2023-10-24 10:07:36.503');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('affab82cc6bd5b210bd67040828800d490edc141a72a7db78562889762517e05', 0, 0,
        '35431c3b3b86e34d30269c9dce37cef6a42d56e4843ca2883e3774ee',
        'bfc661ac04379ab66f11be7762e4aabfd6470755c2d41a1cc488dddca11f0354', 9369000000, 340000000, 234,784774,
        'e05e9a9d2c24788f87299a948c346a85b60b660185ad3241505825a955',
        '["5e9a9d2c24788f87299a948c346a85b60b660185ad3241505825a955"]' FORMAT JSON,
        '[{"ipv4": null, "ipv6": null, "port": 9730, "dnsName": "testicles.kiwipool.org"}]' FORMAT JSON,
        'https://bit.ly/3AT7Sw9', '19dcf5a17af5475da21aae1046a1bdae92ebac5e06e93e8c9a41b7a844fc6af8', 28, 10654460,
        '25c1e9a2e772d3d7dfae2acaa0802e57a4e097ba38e38fdd0cf624dbb13b74ee', 176775, 1666337660,
        '2023-10-24 10:07:40.216');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('4bd374a7849cecf2602f1110e53331a3b305ec668ad57081df98416b30473f5d', 0, 0,
        'b2040523c8f8b1cf6888c7d5ffdfbb94b8bc2e1465f2ad891fdce923',
        'd17c22f1c3617b5e963be0ce7fcf31e26eeb7b4d38b2276eda858d792cf0880f', 9000000000, 340000000, 3,4,
        'e07208fbcf3c12f47ec2b1e1efbb0f8c2c5eff78213b1645205cc95119',
        '["7208fbcf3c12f47ec2b1e1efbb0f8c2c5eff78213b1645205cc95119"]' FORMAT JSON,
        '[{"ipv4": null, "ipv6": null, "port": 6000, "dnsName": "preprod.euskalstakepool.win"}]' FORMAT JSON,
        'https://git.io/JiZgb', '2e421355eb7b4499dd8f0bca206178131f4c9ad90e2bf369ae78aa0c7c4d9472', 28, 10655868,
        'cad170549f5cd56417201746b9a1982c0ad208f9cd92093133e4689096083cb1', 176851, 1666339068,
        '2023-10-24 10:07:40.867');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('8b22e0a6c95f836f766a1de0abdc5bae5c7d86ff47820f00afcafbb55d136cbc', 0, 0,
        '209c6caf1fdabf53984cdcdce4e8d53f680165f33557a47c54b1dd6a',
        '49098bd366f34906068f55b09e019e25cc60af0663b1411ab58c366f7eca037e', 3000000000, 340000000, 1,20,
        'e0d7a4af3ab7479394d57f1b626848ba77ac15319132affd339e64b63c',
        '["d7a4af3ab7479394d57f1b626848ba77ac15319132affd339e64b63c"]' FORMAT JSON,
        '[{"ipv4": null, "ipv6": null, "port": 3001, "dnsName": "pre-prod1.xstakepool.com"}]' FORMAT JSON,
        'https://xstakepool.com/testnet-xstakepool.json',
        '25d14c92cd852bbe666858ba040db7d5dd0767838e604e16c12b8fb842cf89ec', 28, 10656847,
        '6a4b3c5e66f7dc4dff696dcccc2cf7e6688551b039ebc8429c46b02d6cc61396', 176907, 1666340047,
        '2023-10-24 10:07:41.410');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('c9a657446bfd05b9f16b1d168e52e57eabfce4de96ce15e48c4a8c0326b27331', 0, 0,
        '13b76e10523b8964b4053a14782ec1028f67a63f51a1989ea3cd0897',
        '0e2a3dc4f89bba916aa39d61ac90789f53913a7aa6abd2dcabbc9063983038a2', 5000000000, 340000000, 5,666,
        'e070a33897d9601aa080b2f672758cd58b95e0e73e55f635d12a3dffa2',
        '["70a33897d9601aa080b2f672758cd58b95e0e73e55f635d12a3dffa2"]' FORMAT JSON,
        '[{"ipv4": null, "ipv6": null, "port": 6002, "dnsName": "api.monrma.ml"}]' FORMAT JSON,
        'https://api.monrma.ml/meta/PILDR.json', '3f8d0e29efe1017efe9da7ca9cd4fbc18dfbe13ef418a9013a0245a072759e84', 28,
        10661072, '459331a94c0df1acc2a3f34b1514591f64085851fc87f543767ee97e19e8a5e3', 177144, 1666344272,
        '2023-10-24 10:07:43.707');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('45dbc813e19782d6e2384abea975c7c3c84db21360457647714e640e025b8c96', 0, 0,
        'f9c8e7275348d3b1a3596c94095f43307990cc5f800bbbb256298658',
        'd7870e182046358af23ec33b5382415b7464b09adb8f86f319bd644386c5fbfe', 1000000000, 340000000, 1, 10,
        'e0672d9a1544d0642d615314c0aa1973e4b83e40295e7e6cb4d303034b',
        '["672d9a1544d0642d615314c0aa1973e4b83e40295e7e6cb4d303034b"]' FORMAT JSON,
        '[{"ipv4": "88.12.147.44", "ipv6": null, "port": 7001, "dnsName": null}]' FORMAT JSON,
        'https://adamute.com/paltz/poolmeta.json', 'fd4d10c676f8df496084557d261247a42002e09684b5d1f930a94e3f15a73612',
        28, 10664030, 'dd27acb57a2b7a77655dfd1ae46001fbc6276216c8ddfb38aa130ed0a39defa9', 177294, 1666347230,
        '2023-10-24 10:07:44.956');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('d31645b934a3798494563fa3dc39fdd422f5cdab52b39a3b2b84c427ecefcec1', 0, 0,
        'f9c8e7275348d3b1a3596c94095f43307990cc5f800bbbb256298658',
        'd7870e182046358af23ec33b5382415b7464b09adb8f86f319bd644386c5fbfe', 1000000000, 340000000, 1, 10,
        'e0672d9a1544d0642d615314c0aa1973e4b83e40295e7e6cb4d303034b',
        '["672d9a1544d0642d615314c0aa1973e4b83e40295e7e6cb4d303034b"]' FORMAT JSON,
        '[{"ipv4": null, "ipv6": null, "port": 7001, "dnsName": "sp.altzpool.com"}]' FORMAT JSON,
        'https://adamute.com/paltz/poolmeta.json', 'fd4d10c676f8df496084557d261247a42002e09684b5d1f930a94e3f15a73612',
        28, 10664158, '24ae80b7b9bec8becd1d2ab80a4df6d347dee5e99797c27d59e7f427203bd091', 177302, 1666347358,
        '2023-10-24 10:07:45.081');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('061bd9fd1d3a0b520f84750a75b75d85c2eda09eca62231040453e84ed74d9e3', 0, 0,
        '8aa469088eaf5c38c3d4faf0d3516ca670cd6df5545fafea2f70258b',
        'ff9d774cc7e3e85ec1827bfd68c475bc611a9e288e7c9e1fb159fce52d2703fd', 9000000000, 340000000, 0, 1,
        'e0637d965eae29a7ac762e678c070ad15d829933af6e7e98f20c46e5b5',
        '["637d965eae29a7ac762e678c070ad15d829933af6e7e98f20c46e5b5"]' FORMAT JSON,
        '[{"ipv4": "20.90.201.81", "ipv6": null, "port": 3001, "dnsName": null}, {"ipv4": "20.98.184.3", "ipv6": null, "port": 3001, "dnsName": null}]' FORMAT JSON,
        'https://adacapital.io/adact_preprod.json', 'ac5fbc53a3d1493b5ba0ea1772fd5d4fda3cd72ba89503ff2261a39052fcd2f5',
        28, 10670724, 'c3b03252d3a4174eb2421349cc590000fc543f02bf2cef2fda1fbaad4d69154a', 177629, 1666353924,
        '2023-10-24 10:07:48.005');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('b68c4ea03f206a50e64397e8f0a7c0196feecc6de34f39682c87643a9871b4f2', 0, 0,
        'ba2edef7d8bceb725ac92dde87900116a458a32667fe27bb412c49e5',
        'a844e30844acf00049ae40166c4559a345b112d4fda61f0d7b4a12d020aef566', 10000000, 340000000, 1, 5,
        'e01513ed8ec6e793a6570ec43ea461f97a32376514ae2cf41316ff4f2a',
        '["1513ed8ec6e793a6570ec43ea461f97a32376514ae2cf41316ff4f2a"]' FORMAT JSON,
        '[{"ipv4": "51.77.24.220", "ipv6": null, "port": 4002, "dnsName": null}]' FORMAT JSON,
        'https://www.stakecool.io/pools/pre-cool.metadata.json',
        'aade4d000f39df2f9ebfc4e23c9a3c49850236fcb924edc1f4da9be844d9691f', 28, 10674269,
        'a282a4de71219b6e2e0842164a41a1ea41736c693dcab417632c9568ba7cae13', 177784, 1666357469,
        '2023-10-24 10:07:49.274');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('44c46fa5a0abe99d48622b26a60c570f72512bc197257b0b33ec1988ec7da66e', 0, 0,
        'e5ec6131651ca902e7621c796fb90a7357ee5891ef568617bbbcd441',
        'e880658c9e421f3f13036572576120306f2ec8518dbbc2d1d5b464f002ea89f8', 9000000000, 340000000, 767,2333,
        'e0abe10dc699bb9de57303659056a60c087005d7adcf3d1ce7016b6c44',
        '["abe10dc699bb9de57303659056a60c087005d7adcf3d1ce7016b6c44"]' FORMAT JSON,
        '[{"ipv4": "130.61.98.58", "ipv6": null, "port": 3002, "dnsName": null}, {"ipv4": "138.2.174.95", "ipv6": null, "port": 3000, "dnsName": null}]' FORMAT JSON,
        'https://raw.githubusercontent.com/mogog/p2/main/m.json',
        'e25e56e2e039d682520635548d667a8f8170e2246cb5355856fd18e8c13d3989', 28, 10675361,
        '1e6552fea045841b6f03849187af2a08e7865313de60c82e377b40b4e25a967e', 177832, 1666358561,
        '2023-10-24 10:07:49.640');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('b89de0cbb9d062fb578e6c9e8ec72fce9331bb532200991e5841385fb5ce3c96', 0, 0,
        'ad4396a0f7c47e3d69ee8bd792c80ca0bffee61945cec5c42b4ae90f',
        'ef8b7d8610ec49c189729f32273735d249c302d1ee3c42eca018c3f89fcff822', 9500000000, 340000000, 222,2199,
        'e075beb9ff842df716c215316e2518b6f7911ce33e8f66d7c53a58511b',
        '["75beb9ff842df716c215316e2518b6f7911ce33e8f66d7c53a58511b"]' FORMAT JSON,
        '[{"ipv4": "130.61.98.58", "ipv6": null, "port": 3002, "dnsName": null}, {"ipv4": "138.2.174.95", "ipv6": null, "port": 3000, "dnsName": null}]' FORMAT JSON,
        'https://raw.githubusercontent.com/mogog/spm/master/m.json',
        '519ce726299852218cca3cb62a7293246f3aefa5bf78dc5724f27659e584dd43', 28, 10678155,
        'bf986bae80569af22c0376e1c3cd77d5283b5355dc515c8a6972417ca1d9f28e', 177961, 1666361355,
        '2023-10-24 10:07:50.669');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('4e9888c0c9b857f28a772f36473322106537b41ee72baf3dda0054b5e4aee378', 0, 0,
        '30471cd50b3da6601d570e22fec4dda49e143851c1594349a35f13ad',
        '68681468fe7d506331558bd9c5392e120d6e24f2533f0096e563fd88ca56968b', 9000000000, 340000000, 1,2,
        'e0489afd7951769c2f0c6a306c4ed07f1166e3944f563aae38a658241f',
        '["489afd7951769c2f0c6a306c4ed07f1166e3944f563aae38a658241f"]' FORMAT JSON,
        '[{"ipv4": null, "ipv6": null, "port": 9500, "dnsName": "daffy.lbcrypt.com"}]' FORMAT JSON,
        'https://www.lbcrypt.com/json/LBCPP.json', '6074e009fcb59f6ef28337412168e1b5391b42e6cec8dc01f7291babf6d2771d',
        28, 10682695, 'f0c10845e7aca24399b693fcbbcf7c945d7af20c2956689086def16a076ec118', 178177, 1666365895,
        '2023-10-24 10:07:52.483');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('ecd08e5444c08617940e2a1a8a040dfd82b3f271ac1dde67131f48d5820ea857', 0, 0,
        '5564054a595518542c72358e0dc6bc90f10ec7702557b7d67f52b4a9',
        '7ed694fdfc41c29fce3a9eaa100d0951029c1213ae42a31b2de976ec9a9645e8', 100000000, 340000000, 1,100,
        'e04db1404967c6994fe85636cd655d1a8272c53b2c6df7efb9c72fc736',
        '["4db1404967c6994fe85636cd655d1a8272c53b2c6df7efb9c72fc736"]' FORMAT JSON, '[]' FORMAT JSON,
        'https://gitlab.com/santiagopim/spo/-/raw/master/SP001.json',
        'a857c17a49c885ffed5d88cfd76ad861598f72b82e95f0cbeea80a819c7c45db', 28, 10683880,
        '2bee4200e3b1934c756fdf7cf7293a265647d1a02ea665cf10f2948018e8d949', 178244, 1666367080,
        '2023-10-24 10:07:53.115');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('b76f33e4dc546e0f70f4dbac0515cf3d698378eef6f2f63a54847fcbf177f67c', 0, 0,
        'd9f03bcc12a595624049a4950c42fc0920b21b073f84d48c22be964d',
        'f34925452262b97b3745c2b0bea5a723b37648b021058a044a92eb83feb7b180', 2500000000, 340000000, 1,20,
        'e0419aed01b4356815b7a593d768661f1722e4facc449748d6fcf422f4',
        '["419aed01b4356815b7a593d768661f1722e4facc449748d6fcf422f4"]' FORMAT JSON,
        '[{"ipv4": "75.119.144.137", "ipv6": null, "port": 5501, "dnsName": null}, {"ipv4": "85.190.254.156", "ipv6": null, "port": 5501, "dnsName": null}, {"ipv4": null, "ipv6": null, "port": 5501, "dnsName": "relay-s.wota.sbs"}]' FORMAT JSON,
        'https://wota.sbs/metadata_004.json', '80150c19f52a879e710e6cab95cb0508c5c39f7b8ca477716ceccaa7f6a9bf9f', 28,
        10685738, 'ef52af7ff6a1c7731ccea6bc517a73850ceaec4feb45bed805e15f2ae05c9d69', 178326, 1666368938,
        '2023-10-24 10:07:53.927');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('baabdd9d37873fb5e02749141a6e599c1e844ba8dc934941ce488412612d154e', 0, 0,
        '0b5ca58328f3f03438910f055bfd29fc785f8d4ba5bab80b51629da9',
        'e442049ed3f9ac3523cf124b1bf45ce7fb3df65078ab2c157b7bc367f147d2f8', 123000000, 340000000, 0, 0,
        'e08defd48cdbd2659af98d84afdecf53111d4fefbbc1c822fe51eeec2b',
        '["8defd48cdbd2659af98d84afdecf53111d4fefbbc1c822fe51eeec2b"]' FORMAT JSON,
        '[{"ipv4": null, "ipv6": null, "port": 3010, "dnsName": "d.fluxpool.cc"}]' FORMAT JSON,
        'https://cylonyx.github.io/testnet_preprod.json',
        'f6700fd099e403f6535bbba625aeed07d6f95fe96d384f5c8a4a42e552859db4', 28, 10691246,
        '430a1b7b8f32025c7407940f0b12f60565c53869b8326a01d91a094dd0491490', 178582, 1666374446,
        '2023-10-24 10:07:56.039');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('de00fec7c99987b46cc28264650c85d41ddcf06662760009dcb562ee8db0cb65', 0, 0,
        'fdb5834ba06eb4baafd50550d2dc9b3742d2c52cc5ee65bf8673823b',
        '2a6a3d82278a554e9c1777c427bf0397faf5cd7734900752d698e57679cc523f', 5000000000, 340000000, 3, 40,
        'e01aef81cbab75db2de0fe3885332ebe67c34eb1adbf43bb2408ba3981',
        '["1aef81cbab75db2de0fe3885332ebe67c34eb1adbf43bb2408ba3981"]' FORMAT JSON,
        '[{"ipv4": null, "ipv6": null, "port": 3001, "dnsName": "preprod.junglestakepool.com"}]' FORMAT JSON,
        'https://csouza.me/jp-pp.json', 'c9623111188d0bf90e8305e40aa91a040d8036c7813a4eca44e06fa0a1a893a6', 28,
        10705050, 'c5342d7b8abbd29cfd069173d6420da538e900277131eede72a2d18bd654d8c7', 179284, 1666388250,
        '2023-10-24 10:08:02.155');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('fd2759ac8c52afeae8e6d53d022a45c6f3e3313955273adb438167cbc0579e20', 0, 0,
        '429e1cf4c75799b4148402452aa0edd512111485e5e1cbe7cf93b696',
        'ebf3524ed3f2ca11040828fd3cd9aa369866b078dbee272f832facb5ef1e2658', 19000000000, 340000000, 0, 1,
        'e0e821b80fbd30b5592cf1e7c9c41c5f1351b2bc3bf4b54785aa11e2e8',
        '["e821b80fbd30b5592cf1e7c9c41c5f1351b2bc3bf4b54785aa11e2e8"]' FORMAT JSON,
        '[{"ipv4": null, "ipv6": null, "port": 3002, "dnsName": "ppe-testnet-relay.cardanistas.io"}]' FORMAT JSON,
        'https://stakepool.page.link/cards-ppe-testnet-metadata',
        '30fb8325ebda2c1779136b6526d3954531157f1277b2bbb414934453f89eda0a', 28, 10709316,
        '0390c7c1ba1801593e487f0ffdae573a980dd3e739f9b80df5ece51d1d59c9e2', 179526, 1666392516,
        '2023-10-24 10:08:03.843');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('facfb4f680ec2d3da0ef092eb19dd2ca7c9dc820deaabcd0a5bc3341313726d8', 0, 0,
        '4762af74ded2dddd81e2205061e1537a0ad63af01c476727434feaad',
        'aa05fe7b0bad1690fb6ceac182add0dad9e89d10170efa99d789cc390961211b', 100000000, 340000000, 12,333,
        'e01be0283019dc5e612a9c186f2134d73a2c3d40cbad5aaa3600f166c5',
        '["1be0283019dc5e612a9c186f2134d73a2c3d40cbad5aaa3600f166c5"]' FORMAT JSON, '[]' FORMAT JSON,
        'https://gitlab.com/santiagopim/spo/-/raw/master/SP002.json',
        'cec0f467a80e65b9482e7a7a05db8ad15713d4439cadbab96b7f3285fab54916', 28, 10710235,
        'd996d7c2a1e203ce9cf44dbabda8b6ce7043c2d19749fd6e73e7b6921c7fdaa6', 179564, 1666393435,
        '2023-10-24 10:08:04.139');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('89c0c15733ba9577d4a429bb81807e6513f91e29e80c5834824f0575940d1010', 0, 0,
        'd141a247fb9d0e6b31f4e3bc161b0396220cb855e1d96b5eab7f7064',
        '372561ee2032dd1ed1b06e2ef0269801f80b4089c24cea7d5a16383fd548c1c2', 50000000, 340000000, 66,67,
        'e0de08d5b805497c70a1c67ab50e66604e1f7b0afb800aa9fc1138c9dc',
        '["de08d5b805497c70a1c67ab50e66604e1f7b0afb800aa9fc1138c9dc"]' FORMAT JSON,
        '[{"ipv4": null, "ipv6": null, "port": 3001, "dnsName": "preprod.leadstakepool.com"}, {"ipv4": null, "ipv6": null, "port": 3002, "dnsName": "preprod.leadstakepool.com"}]' FORMAT JSON,
        'https://raw.githubusercontent.com/lead-pool/l/master/t.json',
        '8e6568549a2d4b72e29501240abeab5c1ed7cd988965a9e7a81cc3bb438f36b7', 28, 10711808,
        'ee6917d74aaefad30070da203b32b1775cd6ad25783821c87c1e949d43c456a6', 179648, 1666395008,
        '2023-10-24 10:08:04.646');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('93ee0484ab37b88094aaf1f391ef7ddffd9ee8ec19e440ef565a688fa439fb16', 0, 0,
        '0ed90e52db2166b3a80e0711885b94f7950fc0e3898a573a8b98b0e9',
        '318987874684b0cb053dd445b5562d7cbd65d3ff280f8b8877ab6af1bc3e0c4e', 5000000000, 340000000, 3,45,
        'e0ad1333b385cf59d21953e9d6ed7b56ea04e322c3bcc3be356305a029',
        '["ad1333b385cf59d21953e9d6ed7b56ea04e322c3bcc3be356305a029"]' FORMAT JSON,
        '[{"ipv4": null, "ipv6": null, "port": 4001, "dnsName": "relay0-preprod.adanet.io"}]' FORMAT JSON,
        'https://adanet.io/preprodmeta.json', '1170e261a798243562bc1c97fbad735b3fcb3bf37294e2025b5910af9fedbff7', 28,
        10712965, '89fe7c1678be357a4a02271d705a1ff5df40d35a15a2df6931b9c4cc91e5749c', 179703, 1666396165,
        '2023-10-24 10:08:04.962');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('faa22c3b095a08f2e36abc55af4dfa009f55cc95337b81ffffc915e1414303c6', 0, 0,
        'fe662c24cf56fb98626161f76d231ac50ab7b47dd83986a30c1d4796',
        '3606737b41e2c53428f1ad224f8a96b5c73c21028daa1269bad32e8eaa39637c', 5000000000, 340000000, 1, 50,
        'e035befcd685a1a594f74e822217fa422202380e8033258993d6614a74',
        '["35befcd685a1a594f74e822217fa422202380e8033258993d6614a74"]' FORMAT JSON,
        '[{"ipv4": null, "ipv6": null, "port": 17654, "dnsName": "preprodrelay.stakepoolcentral.com"}]' FORMAT JSON,
        'https://bi-preprod.stakepoolcentral.com/CENT.preprod.json',
        'e7d34e25f6b56318fa89f55c11af4547a0c611202aa73b0703b18c62db47f145', 28, 10730322,
        '2067c34cbaee8a343b4d3276ce4b3805b2bef6ba3e796b0751fac9702e525d64', 180546, 1666413522,
        '2023-10-24 10:08:10.731');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('09d4f0ad706a79de35f7a1298c4806548ad2726079a3edc043f29e80a02d6a28', 0, 0,
        '742aafe87d7024ca1f03853f1531f9cee209cd019c92df642e6d7fb9',
        '2db0a696204a8bbef61523723d9c9a99a79cc1549c5e2a0dde3bafe16a272cf7', 8000000, 340000000, 1,12,
        'e011e3f0e6b9d9882a5e5c8865bfb076d917bb0277a02e5f04d4f56363',
        '["11e3f0e6b9d9882a5e5c8865bfb076d917bb0277a02e5f04d4f56363"]' FORMAT JSON,
        '[{"ipv4": null, "ipv6": null, "port": 12000, "dnsName": "test.seaside-staking.best"}]' FORMAT JSON,
        'https://raw.githubusercontent.com/Seaside-Staking/m/main/m.json',
        'd843ac7c8ab3a17fe28d5a68c975dc846fe87479364bcff7dd3b30e5c442ca07', 28, 10739066,
        'c13c33b10b71110279b810515252132e72e2363dc1afea87510b6a677d657226', 180998, 1666422266,
        '2023-10-24 10:08:14.026');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('7f623b6f8330159da9c03890e274481d6c451235162c90b77ae095c6eb3c7688', 0, 0,
        'c997e00dd2174d86a6ed9d814daef42f2dce2babc234b4003bcb02bf',
        '53904d9cbe59b52d6b54ce29a58090ef45b3e44bffdc6ab7c514a7c3934f2a74', 1000000000, 340000000, 1, 30,
        'e0680b6065256bd00c698975a8f27eb8192268e9e079768c625f96fd09',
        '["680b6065256bd00c698975a8f27eb8192268e9e079768c625f96fd09"]' FORMAT JSON,
        '[{"ipv4": null, "ipv6": null, "port": 6000, "dnsName": "lamente.controlliamo.com"}]' FORMAT JSON,
        'https://adasquirrel.com/meta/testnet/poolmeta.json',
        '77ca30b9a2b04150cf3184557c5784558b34415023fd554670ce081d2a878f6d', 28, 10740057,
        '0ba6f1f2c0e963b0251c3ea4861c60686b85914cf4aacd627d3882528eaf5238', 181049, 1666423257,
        '2023-10-24 10:08:14.544');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('d7cbea9325f4f6fd33abe75590926fcce07836268523ab1488f8c8e94fda9725', 0, 0,
        '068c902d6e2305ba416e716a7488cf1d35e4e668986e7d3cc02eb5ca',
        '6ba5623657f3aa518b9ab0fee95d8f080056f3a08663d6fb012f91c38b8ca89a', 5000000000, 340000000, 0, 1,
        'e00daca4418ef9134dec67e14ad10808a0fe304d92c760966f0fd5cc15',
        '["0daca4418ef9134dec67e14ad10808a0fe304d92c760966f0fd5cc15"]' FORMAT JSON,
        '[{"ipv4": "95.216.178.106", "ipv6": null, "port": 3001, "dnsName": null}]' FORMAT JSON,
        'http://vrits.nl/poolmetadata.json', '7c5a05cbd0033914ed2c80cd815548226c41d097ef6384ff232713658272d188', 28,
        10745263, '2078cadb1eca7685721217cf0ee234370a4bcb52967a0ff93d13a7d22ee0f97a', 181341, 1666428463,
        '2023-10-24 10:08:16.250');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('4af97dd0f8be99bb602ffdeea029e50d1913f66b8d878919c87c7417d5faebec', 0, 0,
        '1d9302a3fb4b3b1935e02b27f0339798d3f08a55fbfdcd43a449a96f',
        'f674517bc620a135812231c7975d862d27ff8b9758fe35032d14ef11770c7c03', 0, 10000000000, 1, 10,
        'e0c13582aec9a44fcc6d984be003c5058c660e1d2ff1370fd8b49ba73f',
        '["c13582aec9a44fcc6d984be003c5058c660e1d2ff1370fd8b49ba73f"]' FORMAT JSON,
        '[{"ipv4": "185.164.6.221", "ipv6": null, "port": 54327, "dnsName": null}]' FORMAT JSON,
        'https://my-ip.at/test/preprodpool.metadata.json',
        '975e10c2663c94844d6212e9c71318529367cf7bf5cfddd7877ddd3c74655f16', 28, 10749354,
        'ce4046e37557706496ce5b0e684515a791e84c08bf0b213f8af7d2ddb9accfea', 181552, 1666432554,
        '2023-10-24 10:08:17.378');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('8ca9109ce0a366f8bd5d2c3c1098e860ffc8f8385d1fcaab4f52ca730ea1ece8', 0, 0,
        '5564054a595518542c72358e0dc6bc90f10ec7702557b7d67f52b4a9',
        '7ed694fdfc41c29fce3a9eaa100d0951029c1213ae42a31b2de976ec9a9645e8', 18000000000, 340000000, 1,100,
        'e01be0283019dc5e612a9c186f2134d73a2c3d40cbad5aaa3600f166c5',
        '["4db1404967c6994fe85636cd655d1a8272c53b2c6df7efb9c72fc736"]' FORMAT JSON, '[]' FORMAT JSON,
        'https://gitlab.com/santiagopim/spo/-/raw/master/SP001.json',
        'a857c17a49c885ffed5d88cfd76ad861598f72b82e95f0cbeea80a819c7c45db', 28, 10752909,
        'd7aa65244901c58c8a57e0280afe96c8f7e4e0a0328519c393e40333514de0b1', 181719, 1666436109,
        '2023-10-24 10:08:18.690');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('7ad4566c052e225f070d2303c94e799f9c8734ac52f31f01a5e02f8562c16ce2', 0, 0,
        '9ed23a4a839826d08d7d10f277a91f0e2373ea90251fb33664d52c94',
        '8d765404f01f210d1ba5ce1560dfb8252648f2dc74da9931f1350eeb4dd2c52e', 100000000, 340000000, 1,100,
        'e061c083ba69ca5e6946e8ddfe34034ce84817c1b6a806b112706109da',
        '["61c083ba69ca5e6946e8ddfe34034ce84817c1b6a806b112706109da"]' FORMAT JSON,
        '[{"ipv4": null, "ipv6": null, "port": 6601, "dnsName": "node1.cardano.gratis"}, {"ipv4": null, "ipv6": null, "port": 6602, "dnsName": "node2.cardano.gratis"}]' FORMAT JSON,
        'https://cardano.gratis/poolMetaDataPreProd.json',
        'a021cc8604e4ad9147320f383481fb0faaa4e74f8f49ba5b887f0f41f9299e45', 28, 10753881,
        '3383e12b80fda3b295072b925ff8367e2b8f5dc16546a474efdbe354f3a56377', 181769, 1666437081,
        '2023-10-24 10:08:19.288');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('95e63662f34f3f7f587bee202e162f3e49fa231f07e7981ee36c27e0fad1af57', 0, 0,
        '5564054a595518542c72358e0dc6bc90f10ec7702557b7d67f52b4a9',
        '7ed694fdfc41c29fce3a9eaa100d0951029c1213ae42a31b2de976ec9a9645e8', 18000000000, 340000000, 1,100,
        'e01be0283019dc5e612a9c186f2134d73a2c3d40cbad5aaa3600f166c5',
        '["4db1404967c6994fe85636cd655d1a8272c53b2c6df7efb9c72fc736"]' FORMAT JSON, '[]' FORMAT JSON,
        'https://gitlab.com/santiagopim/spo/-/raw/master/SP001.json',
        '245db43223b860e3d1a1b28a982487686a149ca808a5e18e949053450d4ba5a1', 28, 10754045,
        '4176e0dc1927690afa20a6daf91eef229f0e270ea10c6e7c24f8f630b9be780c', 181776, 1666437245,
        '2023-10-24 10:08:19.372');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('bc40cc86ed43d84d3367a7ff2f4a401dbaed885af96edf1c8fd7379402735699', 0, 0,
        'baba8f13322b33c1ebc2bf1e805bd8ee5300657ff2c0f0749bc892e2',
        '1be7b843e5bc5fbe16e152e6f4586dc5d9857e6bd40d399061042503a8c053d1', 1000000000, 340000000, 1,100,
        'e048937adf2375fe85a4075f6e2898406622fcffdb02acc8a72ff5dd17',
        '["48937adf2375fe85a4075f6e2898406622fcffdb02acc8a72ff5dd17"]' FORMAT JSON,
        '[{"ipv4": "161.97.136.104", "ipv6": null, "port": 3000, "dnsName": null}]' FORMAT JSON,
        'https://tapioca.link/testnet.json', '699cbce25d726970dd5b3579634ae2cd3893181ce63529845e5901a8da7a0e01', 28,
        10762553, 'cd486bded00147c6e3d5333d5a5fe198460ce41adb367f92a959bba706d55cda', 182170, 1666445753,
        '2023-10-24 10:08:23.556');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('34a84652d46b362a092d830c0abf2d921c89ab460d3acb4c44a2c4cfb84d4c20', 0, 0,
        '8b32df703efd84a53ecc38cf5bfb5f42d5eec79b227dccbbe6363e66',
        '32112edb110fe629b9c67e520c1a8375d5969e213efc2f0af29f8bc0762cc286', 5000000000, 340000000, 1,200,
        'e0a36b18435ec5da92ccd6e4447e3ce79e57c875c0d5bffaa96ff861da',
        '["a36b18435ec5da92ccd6e4447e3ce79e57c875c0d5bffaa96ff861da"]' FORMAT JSON,
        '[{"ipv4": null, "ipv6": null, "port": 3003, "dnsName": "relays.techs2help.ch"}, {"ipv4": null, "ipv6": null, "port": 3004, "dnsName": "relays.techs2help.ch"}]' FORMAT JSON,
        'https://raw.githubusercontent.com/Techs2help/T2H/main/pmd.json',
        '87bf2fa0235c6456274b83daebfa4177f839963adee85567f352ac33e7d0a7ed', 28, 10764390,
        'def2633205cd5c733033ecb3a2543903787af10deae02b56487c107d58c91b95', 182272, 1666447590,
        '2023-10-24 10:08:24.634');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('fe196787d61847d15028ff127b07b40f7abac86d3a10c64dc5a50ec7b653dc37', 0, 0,
        'baba8f13322b33c1ebc2bf1e805bd8ee5300657ff2c0f0749bc892e2',
        '1be7b843e5bc5fbe16e152e6f4586dc5d9857e6bd40d399061042503a8c053d1', 28487625262, 340000000, 1,0,
        'e048937adf2375fe85a4075f6e2898406622fcffdb02acc8a72ff5dd17',
        '["48937adf2375fe85a4075f6e2898406622fcffdb02acc8a72ff5dd17"]' FORMAT JSON,
        '[{"ipv4": "161.97.136.104", "ipv6": null, "port": 3000, "dnsName": null}]' FORMAT JSON,
        'https://tapioca.link/testnet.json', '699cbce25d726970dd5b3579634ae2cd3893181ce63529845e5901a8da7a0e01', 28,
        10771694, 'd872f957083798aebadf601788f22e71a191ccba57139992ad865bf89b280d4f', 182634, 1666454894,
        '2023-10-24 10:08:28.911');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('d18b6daad3147fca7c6b243d9119ec7889a5017566a3e64cbf20dfa33b114f55', 0, 0,
        '8aa469088eaf5c38c3d4faf0d3516ca670cd6df5545fafea2f70258b',
        'ff9d774cc7e3e85ec1827bfd68c475bc611a9e288e7c9e1fb159fce52d2703fd', 38000000000, 340000000, 1,100,
        'e0637d965eae29a7ac762e678c070ad15d829933af6e7e98f20c46e5b5',
        '["637d965eae29a7ac762e678c070ad15d829933af6e7e98f20c46e5b5"]' FORMAT JSON,
        '[{"ipv4": "20.90.201.81", "ipv6": null, "port": 3001, "dnsName": null}, {"ipv4": "20.98.184.3", "ipv6": null, "port": 3001, "dnsName": null}]' FORMAT JSON,
        'https://adacapital.io/adact_preprod.json', 'ac5fbc53a3d1493b5ba0ea1772fd5d4fda3cd72ba89503ff2261a39052fcd2f5',
        28, 10785269, 'c2589ab09f7552c32b57dbe05a89d4ee01c46eb1c429e870d42b9fce5786be63', 183253, 1666468469,
        '2023-10-24 10:08:36.305');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('c967a45ebcea9a7f71bb830a01a62449b86f6f3bf7f6304de2e44ce66c761ac0', 0, 0,
        'bd5d42dc1614ebfa575d381e497d25ac8455b0f404b1da86582fe79a',
        'dd110b640b5ca45fb9ef69e0e91847697fb7bf73d9be7965b01d4e12c60360e1', 100000000, 340000000, 0, 0,
        'e0994d32823c804e3a34d79807a055d4d782dcf56d6e3da151a23fdf04',
        '["994d32823c804e3a34d79807a055d4d782dcf56d6e3da151a23fdf04"]' FORMAT JSON,
        '[{"ipv4": null, "ipv6": null, "port": 7050, "dnsName": "damnserver.lambda-honeypot.com"}]' FORMAT JSON,
        'https://tinyurl.com/2p8jcwa2', '72c8528fccab7b0687ea199c5bd6a72a354328ddabe49b902dfbbd64d34ee95e', 28,
        10789144, '5b098c4c6f08428c796aef41c13f90152fdbad74c452e3540721a2fff055c652', 183449, 1666472344,
        '2023-10-24 10:08:38.611');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('a0a5ffd0499cdeb4ce3b3e3670370d25982544491f4b29ad4022f319d213f3aa', 0, 0,
        '10caff4f6980296efccd94b91a966e4692f8fc89de2490605454935a',
        'ed6b09ed118264f29bad9af7b785a48d562998924e2d0a98e9356fcf0a5a01f1', 9000000000, 340000000, 1, 20,
        'e0bc01782caeb91626231228f5c6c579f383de6c1fadab6dc8c3891cb6',
        '["bc01782caeb91626231228f5c6c579f383de6c1fadab6dc8c3891cb6"]' FORMAT JSON,
        '[{"ipv4": null, "ipv6": null, "port": 3001, "dnsName": "preprod.extra-pool.io"}]' FORMAT JSON,
        'http://preprod.extra-pool.io/metadata.json',
        'a54b3ef96a2d87b1a3b4bb85796df6a492b94b7fcc22b037a6b6dbceebc843a6', 28, 10791669,
        'fb74b22b07378db275db91fdb2b8854f9888439c2e4818fd1ccd0a09cac1bc4d', 183564, 1666474869,
        '2023-10-24 10:08:39.984');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('434854a7b27101bdc5fc2159f7667cf21da07f3aeef3e32517ae1fc27b6edf9a', 0, 0,
        '99ebbcff468e4f00be5246f21de5e2db0e8b38f8bc0a7801c658f8cb',
        '141a2392e78ccbf1194c6f4e3ff2e745a52409ba936412e864648903949bf1bd', 9000000000, 340000000, 0, 1,
        'e0eb445bbe365378cf66850fd478f63835eae3b417fdfb004fdda38a3a',
        '["eb445bbe365378cf66850fd478f63835eae3b417fdfb004fdda38a3a"]' FORMAT JSON,
        '[{"ipv4": null, "ipv6": null, "port": 3001, "dnsName": "preprod.bladepool.com"}]' FORMAT JSON,
        'https://bladepool.com/metadata.json',
        '2738e2233800ab7f82bd2212a9a55f52d4851f9147f161684c63e6655bedb562', 28, 10811653,
        '64bde349c8dd00ee6195217d36e4449f6c64f3b404fed17ad284cad3f810cce2', 184626, 1666494853,
        '2023-10-24 10:08:51.416');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('fe8d4a7b8aaf139122dafea596fcab2f4cd4958aeaee3e13a4a8ec7b8c4a5576', 0, 0,
        '0ed90e52db2166b3a80e0711885b94f7950fc0e3898a573a8b98b0e9',
        '318987874684b0cb053dd445b5562d7cbd65d3ff280f8b8877ab6af1bc3e0c4e', 5000000000, 340000000, 3, 40,
        'e0ad1333b385cf59d21953e9d6ed7b56ea04e322c3bcc3be356305a029',
        '["ad1333b385cf59d21953e9d6ed7b56ea04e322c3bcc3be356305a029"]' FORMAT JSON,
        '[{"ipv4": null, "ipv6": null, "port": 4001, "dnsName": "relay0-preprod.adanet.io"}]' FORMAT JSON,
        'https://adanet.io/preprodmeta.json', '1170e261a798243562bc1c97fbad735b3fcb3bf37294e2025b5910af9fedbff7', 28,
        10817949, 'f90e0da01d050acd408a6b0f2d725ef04d34b60c4c19db8dc715c4e4a866d1be', 184937, 1666501149,
        '2023-10-24 10:08:54.748');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('1bfaf5e813460496b4960e5d5dc5846caa468d604c5754d1e3e262cf08b76d58', 0, 0,
        '0ed90e52db2166b3a80e0711885b94f7950fc0e3898a573a8b98b0e9',
        '318987874684b0cb053dd445b5562d7cbd65d3ff280f8b8877ab6af1bc3e0c4e', 5000000000, 340000000, 3, 40,
        'e0ad1333b385cf59d21953e9d6ed7b56ea04e322c3bcc3be356305a029',
        '["ad1333b385cf59d21953e9d6ed7b56ea04e322c3bcc3be356305a029"]' FORMAT JSON,
        '[{"ipv4": null, "ipv6": null, "port": 4001, "dnsName": "relay0-preprod.adanet.io"}]' FORMAT JSON,
        'https://adanet.io/preprodmeta.json', '2da0f075afcc0b1501ac1612d80dca04156a44f8f3722126cbb5baaf93d7447d', 28,
        10818330, 'ff04b3e09ab78415b050fd5ff32949d1c23addd55085681e1de862d1268b63f3', 184956, 1666501530,
        '2023-10-24 10:08:54.944');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('320b4c0ca244f736adc43d6038cbcfda359070a8c0585e0dedfa5e607c1e5abb', 0, 0,
        '0ed90e52db2166b3a80e0711885b94f7950fc0e3898a573a8b98b0e9',
        '318987874684b0cb053dd445b5562d7cbd65d3ff280f8b8877ab6af1bc3e0c4e', 5000000000, 340000000, 3, 40,
        'e0ad1333b385cf59d21953e9d6ed7b56ea04e322c3bcc3be356305a029',
        '["ad1333b385cf59d21953e9d6ed7b56ea04e322c3bcc3be356305a029"]' FORMAT JSON,
        '[{"ipv4": null, "ipv6": null, "port": 4001, "dnsName": "relay0-preprod.adanet.io"}]' FORMAT JSON,
        'https://adanet.io/preprodmeta.json', 'e908c3a40a7850da314e59d1436885a2ccbc461bef46f1146e6bbd08f5b02c27', 28,
        10818515, '97155167b2fd65680c60b2c7014b7d67cb3e742a455aa458f6e1c4a42765b7b1', 184964, 1666501715,
        '2023-10-24 10:08:55.039');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('2c8a8f97450e0f313e2eba11ced6efe98623d88855f076b19e0e205431ad44ed', 0, 0,
        'f6c1aff2a4c000f8e415f140f97d282e8871bf050a2699c7f109fa8b',
        'a6254fe6ad6c49045b9a19a4669d94f0bdebe905a2c55b10e361373a4af7dc97', 100000000, 345000000, 3, 400,
        'e07f940c316af9798f45f9f48d5d854c1c34df50bec4435c40894532b2',
        '["7f940c316af9798f45f9f48d5d854c1c34df50bec4435c40894532b2"]' FORMAT JSON,
        '[{"ipv4": null, "ipv6": null, "port": 6000, "dnsName": "95.216.154.243"}]' FORMAT JSON,
        'http://smm.jnology.net/poolMetaData.json', 'afdf5aa90ad33073ed75f0a72bc72b952e774deac00e7f4cb1020bfb7c6e91ef',
        28, 10832480, '41244fdb9ccfd0904f4687575e758dbd3b67a08f0fd4c96f1f841fa811b1ea07', 185615, 1666515680,
        '2023-10-24 10:09:03.774');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('88e0c396c4a1fbe199974156fabb857ea5b2b93c8e251270d762485c12ea2ef0', 0, 0,
        '5564054a595518542c72358e0dc6bc90f10ec7702557b7d67f52b4a9',
        '7ed694fdfc41c29fce3a9eaa100d0951029c1213ae42a31b2de976ec9a9645e8', 38000000000, 340000000, 3, 40,
        'e01be0283019dc5e612a9c186f2134d73a2c3d40cbad5aaa3600f166c5',
        '["4db1404967c6994fe85636cd655d1a8272c53b2c6df7efb9c72fc736"]' FORMAT JSON, '[]' FORMAT JSON,
        'https://gitlab.com/santiagopim/spo/-/raw/master/SP001.json',
        '245db43223b860e3d1a1b28a982487686a149ca808a5e18e949053450d4ba5a1', 28, 10839541,
        '9fd6f9ea735fb7ee52fc0185371a421b9b27c48d59b775e77e00023ad93c0fb5', 185987, 1666522741,
        '2023-10-24 10:09:08.540');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('0a191240dcd341d78cc8d7c1c5df0a6d1ae3992cc4d999c4968802352a19ddb0', 0, 0,
        'ad4396a0f7c47e3d69ee8bd792c80ca0bffee61945cec5c42b4ae90f',
        'ef8b7d8610ec49c189729f32273735d249c302d1ee3c42eca018c3f89fcff822', 100000000000, 340000000, 1, 10,
        'e075beb9ff842df716c215316e2518b6f7911ce33e8f66d7c53a58511b',
        '["75beb9ff842df716c215316e2518b6f7911ce33e8f66d7c53a58511b"]' FORMAT JSON,
        '[{"ipv4": "130.61.98.58", "ipv6": null, "port": 3002, "dnsName": null}, {"ipv4": "138.2.174.95", "ipv6": null, "port": 3000, "dnsName": null}]' FORMAT JSON,
        'https://raw.githubusercontent.com/mogog/spm/master/m.json',
        '519ce726299852218cca3cb62a7293246f3aefa5bf78dc5724f27659e584dd43', 28, 10841786,
        '07370096bb2067aa2482acae3a46fe89f5a59c52fc1d5cbd4286435a19306aa7', 186105, 1666524986,
        '2023-10-24 10:09:10.119');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('7340e047f596b91ce9679cf9c51272b6d613b35fb1a4711d924409ae6e2d3275', 0, 0,
        'ad4396a0f7c47e3d69ee8bd792c80ca0bffee61945cec5c42b4ae90f',
        'ef8b7d8610ec49c189729f32273735d249c302d1ee3c42eca018c3f89fcff822', 100000000000, 340000000, 0, 1,
        'e075beb9ff842df716c215316e2518b6f7911ce33e8f66d7c53a58511b',
        '["75beb9ff842df716c215316e2518b6f7911ce33e8f66d7c53a58511b"]' FORMAT JSON,
        '[{"ipv4": "130.61.98.58", "ipv6": null, "port": 3002, "dnsName": null}, {"ipv4": "138.2.174.95", "ipv6": null, "port": 3000, "dnsName": null}]' FORMAT JSON,
        'https://raw.githubusercontent.com/mogog/spm/master/m.json',
        '519ce726299852218cca3cb62a7293246f3aefa5bf78dc5724f27659e584dd43', 28, 10841830,
        '6362e6d7a99a7dbe42bd3f3c37147ff2d21494e5d44ba6c6dd440bda48fa2b8e', 186106, 1666525030,
        '2023-10-24 10:09:10.147');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('e8524c9d3e4c073baaf3cb216b5744a59d88ac38d417ddd24ed2b11e1fffcc57', 0, 0,
        'ad4396a0f7c47e3d69ee8bd792c80ca0bffee61945cec5c42b4ae90f',
        'ef8b7d8610ec49c189729f32273735d249c302d1ee3c42eca018c3f89fcff822', 100000000000, 340000000, 0, 1,
        'e075beb9ff842df716c215316e2518b6f7911ce33e8f66d7c53a58511b',
        '["75beb9ff842df716c215316e2518b6f7911ce33e8f66d7c53a58511b"]' FORMAT JSON,
        '[{"ipv4": "130.61.98.58", "ipv6": null, "port": 3002, "dnsName": null}, {"ipv4": "138.2.174.95", "ipv6": null, "port": 3000, "dnsName": null}]' FORMAT JSON,
        'https://raw.githubusercontent.com/mogog/spm/master/m.json',
        '519ce726299852218cca3cb62a7293246f3aefa5bf78dc5724f27659e584dd43', 28, 10841956,
        '2f4003768984d5f3c0bd5ff5c8bdd4e06e51af9b0f5a976f238306569da8cea3', 186114, 1666525156,
        '2023-10-24 10:09:10.256');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('b494649e06ea3f76791da02c2124573c506c7a2bfad1070983efe510c903fb5a', 0, 0,
        '0362558dc8a66643368d9706ef1a201083b20dafd4b26f1a77319d33',
        'dcfcb55bf3b4bdc1e4009ae2ac6c763fb0d5c9a8053a9a7edadd071ca4807d56', 100000000000, 500000000, 0, 1,
        'e0277a7cd19eb2444f2a3e05c837233b5c85198f698688a8e5139787c1',
        '["277a7cd19eb2444f2a3e05c837233b5c85198f698688a8e5139787c1"]' FORMAT JSON,
        '[{"ipv4": "10.1.52.236", "ipv6": null, "port": 3001, "dnsName": null}]' FORMAT JSON,
        'https://adapublicmetadata.s3.eu-west-2.amazonaws.com/1.json',
        'aeaaf8236c5db042e4d3131ebbd183bd26cf615712d0fad1477ff70cb61e287a', 28, 10846630,
        'd58bb4aba13acef975368f3304813532079a57fdf7ac6bd9d64844bc488b1585', 186335, 1666529830,
        '2023-10-24 10:09:13.110');
INSERT INTO pool_registration
(tx_hash, cert_index, tx_index, pool_id, vrf_key, pledge, cost, margin_numerator, margin_denominator, reward_account, pool_owners, relays, metadata_url,
 metadata_hash, epoch, slot, block_hash, block, block_time, update_datetime)
VALUES ('4ac12a91f3c0052bad5076a393db5da5a2b4b934842f2262c249a948fc11fc86', 0, 0,
        '54977486bc076ef24dab6f34652c1d33113f871ebc5e863b2488e03c',
        'd1b243a8f5b0dadbf54755bd438e199dcf08ef9c6ff6b16add73b4d4da23dff5', 10000000000, 340000000, 0, 1,
        'e0eaba4ef8c926aeed3c9785eaf0dbdc8f24953b43559401c7c14eccc7',
        '["eaba4ef8c926aeed3c9785eaf0dbdc8f24953b43559401c7c14eccc7"]' FORMAT JSON,
        '[{"ipv4": null, "ipv6": null, "port": 8001, "dnsName": "test-relay1.0aaaa.org"}, {"ipv4": null, "ipv6": null, "port": 8002, "dnsName": "test-relay2.0aaaa.org"}]' FORMAT JSON,
        'https://a4a.s3.eu-north-1.amazonaws.com/mdt.json',
        'e5ce50080872955eaea4bf9184deafb04cadc91c926379be5b4324a0399cd894', 28, 10848675,
        '4be9c649b57f5d437bfd6b54dd147de78828391373c1780cf38a10e7f4215879', 186446, 1666531875,
        '2023-10-24 10:09:14.723');
