#!/bin/bash

echo "Prebuild"

echo "Downloading golang"

cd "$HOME"
curl -L https://go.dev/dl/go1.23.3.linux-amd64.tar.gz > go.tar.gz
tar -xf go.tar.gz

