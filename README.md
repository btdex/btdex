# BTDEX - BlockTalk Decentralized Exchange reference client

![](https://github.com//btdex/btdex/workflows/BTDEX%20Build/badge.svg)
[![GPLv3](https://img.shields.io/badge/license-GPLv3-blue.svg)](LICENSE)

BTDEX is a decentralized exchange system running on the [Burst](https://www.burst-coin.org/) blockchain.
It implements a unique non-custodial exchange method for cryptocurrencies and conventional fiat currencies based on [BlockTalk](https://github.com/jjos2372/blocktalk) Smart Contracts and Burstcoin on-chain encrypted messages.
The exchange method is serverless and fees are distributed among [Trade Token (TRT)](https://explore2.burstcoin.ro/asset/12402415494995249540) holders.

BTDEX is currently on *initial token distribution*, more details at [https://btdex.trade](https://btdex.trade).

## Download

Check the [releases](https://github.com/btdex/btdex/releases) and get the latest one.

## Compile from source

Clone this repository code and run the gradle build:

`$ ./gradlew shadowJar`

This will result on the following file (runnable with Java 8 or more recent):

`build/libs/btdex-all.jar`

To build the windows executable run:

`$ ./gradlew createExe`

This will result on the following file:

`build/launch4j/btdex.exe`

## License
[GPL license](LICENSE)

## Author
jjos

Donation address: BURST-JJQS-MMA4-GHB4-4ZNZU
