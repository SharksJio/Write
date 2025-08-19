#!/bin/bash

# Verification script for Android Native UI implementation
# This checks that all required files are in place and properly configured

set -e

echo "🔍 Verifying Android Native UI implementation..."

# Check required Java files
echo "✅ Checking Java files..."
required_java_files=(
    "syncscribble/android/app/src/main/java/com/styluslabs/writeqt/NativeActivity.java"
    "syncscribble/android/app/src/main/java/com/styluslabs/writeqt/NativeCanvasView.java"
)

for file in "${required_java_files[@]}"; do
    if [ -f "$file" ]; then
        echo "  ✓ $file"
    else
        echo "  ✗ $file (missing)"
        exit 1
    fi
done

# Check required native files
echo "✅ Checking Native files..."
required_native_files=(
    "syncscribble/android/native_bridge.cpp"
)

for file in "${required_native_files[@]}"; do
    if [ -f "$file" ]; then
        echo "  ✓ $file"
    else
        echo "  ✗ $file (missing)"
        exit 1
    fi
done

# Check required layout files
echo "✅ Checking Layout files..."
required_layout_files=(
    "syncscribble/android/app/src/main/res/layout/activity_main.xml"
)

for file in "${required_layout_files[@]}"; do
    if [ -f "$file" ]; then
        echo "  ✓ $file"
    else
        echo "  ✗ $file (missing)"
        exit 1
    fi
done

# Check AndroidManifest.xml uses NativeActivity
echo "✅ Checking AndroidManifest.xml..."
if grep -q "NativeActivity" syncscribble/android/app/src/main/AndroidManifest.xml; then
    echo "  ✓ AndroidManifest.xml configured for NativeActivity"
else
    echo "  ✗ AndroidManifest.xml not updated"
    exit 1
fi

# Check Makefile has ANDROID_NATIVE_UI support
echo "✅ Checking Makefile..."
if grep -q "ANDROID_NATIVE_UI" syncscribble/Makefile; then
    echo "  ✓ Makefile supports ANDROID_NATIVE_UI mode"
else
    echo "  ✗ Makefile not updated"
    exit 1
fi

# Check application.cpp has native entry point
echo "✅ Checking application.cpp..."
if grep -q "android_native_main" syncscribble/application.cpp; then
    echo "  ✓ application.cpp has native entry point"
else
    echo "  ✗ application.cpp not updated"
    exit 1
fi

echo ""
echo "🎉 All verification checks passed!"
echo ""
echo "📋 Summary of changes:"
echo "   • Created NativeActivity.java to replace SDL-based MainActivity"
echo "   • Created NativeCanvasView.java for drawing surface"
echo "   • Created native_bridge.cpp for JNI integration"
echo "   • Created activity_main.xml for native Android layout"
echo "   • Updated AndroidManifest.xml to use NativeActivity"
echo "   • Updated Makefile to support ANDROID_NATIVE_UI build mode"
echo "   • Updated application.cpp with native entry point"
echo ""
echo "🚀 Ready to build with: cd syncscribble/android && ANDROID_NATIVE_UI=1 ./gww assembleRelease"