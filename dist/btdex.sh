#!/bin/bash

CONFIG_DIR="$HOME/.config/btdex"

mkdir -p $CONFIG_DIR

/opt/btdex/jre/bin/java -jar /opt/btdex/btdex-all.jar "$@"
