#!/bin/bash

echo "Build"

export PATH="$PATH:$HOME/go/bin"
export NDK="$FDROID_NDK"

echo $PATH
echo $NDK

./gradlew assembleFdroid

