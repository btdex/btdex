# BTDEX - decentralized exchange, wallet, plotting, and mining tool

![](https://github.com//btdex/btdex/workflows/BTDEX%20Build/badge.svg)
[![GPLv3](https://img.shields.io/badge/license-GPLv3-blue.svg)](LICENSE)

BTDEX is a decentralized exchange system running on the [Signum](https://signum.network/) blockchain.
It implements a unique non-custodial exchange method for cryptocurrencies and conventional fiat currencies based on [BlockTalk](https://github.com/jjos2372/blocktalk) Smart Contracts and Signum on-chain encrypted messages.
The exchange method is serverless and fees are distributed among [Trade Token (TRT)](https://explore.burstcoin.ro/asset/12402415494995249540) holders.

You will also find more details at [https://btdex.trade](https://btdex.trade).
Currently the following pairs are available with BURST:
 - BTC
 - ARRR
 - BCH
 - BNB
 - BSV
 - DOGE
 - ETH
 - LTC
 - XMR
 - XLA

 
Additionally, any Signum-based token can be listed instantly and traded.

## Download

Check the [releases](https://github.com/btdex/btdex/releases) and get the latest one.

### Running on Windows
Just download the `btdex-version-win_x64.zip` [latest release](https://github.com/btdex/btdex/releases) and extract it
to a folder you have write rights.
Double click on `btdex.exe` to start the application (it will create a file named `config.properties` inside a `btdex` folder in the [user %APPDATA%](https://www.howtogeek.com/318177/what-is-the-appdata-folder-in-windows/) or use a file with that name if available on the same folder).

### Running on Linux

#### Ubuntu and other Debian-based distributions
Just download the `btdex_version_amd64.deb` [latest release](https://github.com/btdex/btdex/releases) and install it.
The application `BTDEX` will be available on the system (config file will go to `.config/btdex/` inside your home folder).

### Running on MacOS
Just download the `btdex-mac-version.zip` [latest release](https://github.com/btdex/btdex/releases) and uncompress the app.
You can now run the app as usual (config file will go to `.config/btdex/` inside your home folder).

### General method without installing
Just download the `btdex-version-all.jar` [latest release](https://github.com/btdex/btdex/releases) and copy it
to a folder you have write rights (it will create a file named `config.properties` with your account details as described for different operating systems above).
Run this jar file with Java 8 or 11 (the `xdg-utils` package is required to open your browser when necessary):

`java -jar btdex-version-all.jar`

## Translations
If you want to see BTDEX on your own language or have suggestions on how to improve a translation, please join us at https://www.transifex.com/btdex/.

## Compile from source

Clone this repository code and run the gradle build (requires Java 8 to build):

```
$ git clone https://github.com/btdex/btdex.git
$ cd btdex
$ ./gradlew release
```

The many resulting files will be found inside the `build` folder, like:

`build/libs/btdex-all.jar`

The Windows package will be found at:

`build/distributions/btdex-win_x64.zip`

## Running on testnet

Edit your `config.properties` file and add the following lines:

```
testnet=True
node=http\://nivbox.co.uk\:6876
```

## API

You can enable the API by adding the following line on your `config.properties` file:

```
apiPort=9000
```

You can choose a port number other than 9000. The config file is usually found on the same folder where the BTDEX application is executed. If you installed a DEB (Linux) the config file is found at `~/.config/btdex`. The API will be available at `http://localhost:9000/api/v1/`, with the following endpoints:
 - `http://localhost:9000/api/v1/summary`
 - `http://localhost:9000/api/v1/ticker`
 - `http://localhost:9000/api/v1/orderbook/SIGNA_TRT` (or any other pair listed on the *summary*)
 - `http://localhost:9000/api/v1/trades/SIGNA_TRT` (or any other pair listed on the *summary*)

## Logging

By default, logging is disabled. Add/edit the following line on your `config.properties` to change the logging level:

```
logger=off
```

### Logging level

The same logging level is used to print log messages to the console and as well as to log file, possible levels are:

- `OFF` The highest possible log level. This is intended for disabling logging.
- `FATAL` Indicates server errors that cause premature termination. These logs are expected to be immediately visible on the command line that you used for starting the server.
- `ERROR` Indicates other runtime errors or unexpected conditions. These logs are expected to be immediately visible on the command line that you used for starting the server.
- `WARN` Indicates the use of deprecated APIs, poor use of API, possible errors, and other runtime situations that are undesirable or unexpected but not necessarily wrong. These logs are expected to be immediately visible on the command line that you used for starting the server.
- `INFO` Indicates important runtime events, such as server startup/shutdown. These logs are expected to be immediately visible on the command line that you used for starting the server . It is recommended to keep these logs to a minimum.
- `DEBUG` Provides detailed information on the flow through the system. This information is expected to be written to logs only. Generally, most lines logged by your application should be written as DEBUG logs.
- `TRACE` Provides additional details on the behavior of events and services. This information is expected to be written to logs only.<br>

## License
[GPL license](LICENSE)

## Author
jjos

Donation address: S-JJQS-MMA4-GHB4-4ZNZU
