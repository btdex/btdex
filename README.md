# BTDEX - BlockTalk Decentralized Exchange reference client

![](https://github.com//btdex/btdex/workflows/BTDEX%20Build/badge.svg)
[![GPLv3](https://img.shields.io/badge/license-GPLv3-blue.svg)](LICENSE)

BTDEX is a decentralized exchange system running on the [Burst](https://www.burst-coin.org/) blockchain.
It implements a unique non-custodial exchange method for cryptocurrencies and conventional fiat currencies based on [BlockTalk](https://github.com/jjos2372/blocktalk) Smart Contracts and Burstcoin on-chain encrypted messages.
The exchange method is serverless and fees are distributed among [Trade Token (TRT)](https://explore.burstcoin.ro/asset/12402415494995249540) holders.

BTDEX is currently on *initial token distribution*, more details at [https://btdex.trade](https://btdex.trade).

## Download

Check the [releases](https://github.com/btdex/btdex/releases) and get the latest one.

### Running on Windows
Just download the `btdex-version.exe` [latest release](https://github.com/btdex/btdex/releases) and copy it
to a folder you have write rights (it will create a file named `config.properties` with your account details).
Double click on `btdex-version.exe` to start the application.

### Running on Linux

#### Ubuntu and other Debian-based distributions
Just download the `btdex_version_all.deb` [latest release](https://github.com/btdex/btdex/releases) and install it.
The application `BTDEX` will be available on the system (config file will go to `.config/btdex/` inside your home folder).

#### Archlinux
A package is available at [AUR](https://aur.archlinux.org/packages/btdex/).

### Running on MacOS
Just download the `btdex-mac-version.zip` [latest release](https://github.com/btdex/btdex/releases) and uncompress the app.
You can now run the app as usual (it will create a file named `config.properties` inside your home folder `~/`).

### General method without installing
Just download the `btdex-all-version.jar` [latest release](https://github.com/btdex/btdex/releases) and copy it
to a folder you have write rights (it will create a file named `config.properties` with your account details).
Run this jar file with Java 8 or more recent (the `xdg-utils` package is required to open your browser when necessary):

`java -jar btdex-all-version.jar`

## Compile from source

Clone this repository code and run the gradle build (requires Java 8 to build):

```
$ git clone https://github.com/btdex/btdex.git
$ cd btdex
$ ./gradlew shadowJar
```

This will result on the following file:

`build/libs/btdex-all.jar`

To build the windows executable run:

`$ ./gradlew createExe`

This will result on the following file:

`build/launch4j/btdex.exe`

## Running on testnet

Edit your `config.properties` file and add the following lines:

```
testnet=True
node=http\://testnet.getburst.net\:6876
```

## License
[GPL license](LICENSE)

## Author
jjos

Donation address: BURST-JJQS-MMA4-GHB4-4ZNZU
