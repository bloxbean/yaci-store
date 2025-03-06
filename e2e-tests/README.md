# Overview

This module helps to run various end-to-end scenarios with Yaci DevKit devnet.

Before running these tests, you need to start Yaci DevKit as a separate process with a few changes to the `node.properties` file.

Currently, starting Yaci DevKit is a manual process. However, we can use Yaci DevKit's `reset` admin endpoint to reset the network in the test class, if required.

In the future, we can automate this process using the [Yaci Cardano Test](https://github.com/bloxbean/yaci-cardano-test) project.

### 1. Update `node.properties`

- For the Yaci DevKit Docker version, `node.properties` can be found in the `~/.yaci-devkit/config` folder.
- For the non-Docker version, it can be found in the `config` sub-folder under the `yaci-cli` folder.

 #### a. Comment the following two properties, as they can impact the stability window, which is critical for reward calculation.

```properties
#conwayHardForkAtEpoch=1
#shiftStartTimeBehind=true

```

 #### b. Adjust the governance parameters to reduce the govActionLifetime and configure other settings for auto-approval of proposals.
The following parameters can be adjusted based on test cases. 
Currently, this is a manual process, but we will explore how to do this dynamically using the reset command later, depending on different scenarios.

```
govActionLifetime=3
dvtTreasuryWithdrawal=0
committeeMinSize=0

ccThresholdNumerator=0
ccThresholdDenominator=1
```

### 2. Start the devnet with epoch length 50.

```
create-node -o --start --epoch-length 50
```

### 3. Dynamically Update Configuration in Test Cases (Currently, only with Yaci Devkit main branch build)

Yaci DevKit provides a `create` admin endpoint which can be used to initialize a devnet with a different configuration and start.
This is useful when we want to verify various configuration scenario in our test cases.
