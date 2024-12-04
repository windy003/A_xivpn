#!/bin/bash

echo "Build"

export PATH="$PATH:$HOME/go/bin"
export NDK="$FDROID_NDK"

# Build native library
cd libxivpn

echo "Building native library"
go version

NDK=$NDK/toolchains/llvm/prebuilt/linux-x86_64 ./build.sh
mkdir ../app/src/main/jniLibs/arm64-v8a
cp libxivpn_arm64.so ../app/src/main/jniLibs/arm64-v8a/libxivpn.so
mkdir ../app/src/main/jniLibs/x86_64
cp libxivpn_x86_64.so ../app/src/main/jniLibs/x86_64/libxivpn.so

# Build APK
cd ..

echo "Building APK..."
gradle assembleFdroid

