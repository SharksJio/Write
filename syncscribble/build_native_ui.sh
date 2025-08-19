#!/bin/bash

# Build script for Android Native UI version
# This builds the Write app using native Android UI instead of SDL

set -e  # Exit on any error

echo "Building Write for Android with Native UI..."

cd "$(dirname "$0")"

# Set environment variable to enable native UI mode
export ANDROID_NATIVE_UI=1

# Build the native library
echo "Building native library..."
cd android
./gww assembleArm64rel

echo "Build completed!"
echo "APK location: android/app/build/outputs/apk/arm64rel/"