#!/bin/bash

echo "Prebuild"

if [ ! -f "$HOME/go/bin/go" ]; then
    cd $HOME
    echo "Downloading golang"
    curl -L https://go.dev/dl/go1.23.3.linux-amd64.tar.gz > go.tar.gz
    tar -xf go.tar.gz
    ls $HOME/go/bin/go
fi


