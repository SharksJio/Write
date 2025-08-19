# Write Android APK Build Instructions

This document provides comprehensive instructions for building an Android APK from the Write application source code, including the changes made in this PR.

## Changes in This PR

The current PR includes:
1. **CI/CD Workflow**: Added GitHub Actions workflow for continuous integration
2. **Build Environment Setup**: Gradle wrapper and Android build configuration improvements
3. **Font Location Fix**: Addresses font location issues for clean checkout

## Prerequisites

### Required Software
- **Android SDK**: Version 34 or later
- **Android NDK**: Version 26.3.11579264 (as specified in build.gradle)
- **Java**: OpenJDK 17 or later
- **Git**: For cloning submodules

### System Dependencies (Ubuntu/Debian)
```bash
sudo apt-get update
sudo apt-get install -y openjdk-17-jdk git curl unzip
```

## Setup Instructions

### 1. Install Android SDK
```bash
# Create Android SDK directory
mkdir $HOME/android-sdk && cd $HOME/android-sdk

# Download command line tools
curl https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip --output cmdline-tools.zip
unzip cmdline-tools.zip

# Install required SDK components
cmdline-tools/bin/sdkmanager --sdk_root=. --install \
    'build-tools;34.0.0' \
    'platforms;android-34' \
    'platform-tools' \
    'ndk;26.3.11579264' \
    'cmake;3.18.1'

# Set environment variable
export ANDROID_HOME="$HOME/android-sdk"
```

### 2. Clone Repository with Submodules
```bash
git clone --recurse-submodules https://github.com/SharksJio/Write.git
cd Write

# Ensure SDL is on the correct branch (if needed)
cd SDL
git checkout write-android
cd ..
```

### 3. Build Setup
```bash
cd syncscribble/android

# Set Android SDK path
export ANDROID_HOME="$HOME/android-sdk"

# Make build script executable
chmod +x gww

# Download gradle wrapper (if not already present)
./gww --version
```

## Building the APK

### Option 1: Using Gradle (Recommended)
```bash
cd syncscribble/android

# Build debug APK
./gww assembleDebug

# Build release APK
./gww assembleRelease

# Build and install on connected device
./gww installRelease
```

### Option 2: Alternative Build Commands
```bash
# Using start_gradle script (for developers familiar with the original setup)
./start_gradle assembleRelease

# The built APK will be located at:
# app/build/outputs/apk/release/app-release.apk
```

## APK Signing (Optional)

For distribution outside of debug testing, you'll need to sign the APK:

```bash
# Copy the built APK
cp app/build/outputs/apk/release/app-release.apk .

# Sign using the provided script (requires keystore)
./resignapk.sh app-release.apk ~/your-keystore.keystore

# This creates signed_app-release.apk
```

## Build Variants

The project supports multiple build variants:

- **debug**: Development build with debugging enabled
- **release**: Optimized release build
- **arm64rel**: ARM64-only release build
- **x86rel**: x86-only release build

### Building Specific Variants
```bash
# ARM64 only
./gww assembleArm64rel

# x86 only  
./gww assembleX86rel
```

## Troubleshooting

### Common Issues

1. **Gradle Download Failures**
   - Ensure internet connection is available
   - Check proxy settings if behind corporate firewall

2. **NDK Not Found**
   - Verify ANDROID_HOME is set correctly
   - Ensure NDK version 26.3.11579264 is installed

3. **Submodule Issues**
   - Run `git submodule update --init --recursive`
   - Verify SDL branch is set to 'write-android'

4. **Build Failures**
   - Clean build: `./gww clean`
   - Check Android SDK installation
   - Verify all dependencies are installed

### Debugging Build Issues
```bash
# Verbose build output
./gww assembleDebug --info

# Stack trace on failures
./gww assembleDebug --stacktrace

# Debug gradle daemon
./gww --stop
./gww assembleDebug --debug
```

## Expected Output

A successful build will produce:
- **Debug APK**: `app/build/outputs/apk/debug/app-debug.apk`
- **Release APK**: `app/build/outputs/apk/release/app-release.apk`

The APK will be approximately 15-25 MB in size and contain:
- Native libraries for ARM64, ARMv7, and x86_64 architectures
- Application resources and assets
- Write application with all features from the current codebase

## Testing the APK

1. **Install on Device**
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

2. **Verify Installation**
   - Look for "Write" app in device app drawer
   - Launch app and verify it starts without crashes
   - Test basic functionality like creating a new document

3. **Check Logs**
   ```bash
   adb logcat | grep -e Tangram -e StylusLabs
   ```

## Project Structure

```
syncscribble/android/
├── app/
│   ├── build.gradle          # App-level build configuration
│   └── src/main/
│       ├── java/             # Java source files
│       ├── jni/              # Native code (Android.mk)
│       └── res/              # Android resources
├── build.gradle              # Project-level build configuration
├── settings.gradle           # Gradle settings
├── gww                       # Gradle wrapper script
└── resignapk.sh             # APK signing script
```

## Additional Notes

- The build process requires approximately 2-4 GB of disk space
- Initial build may take 10-15 minutes depending on system performance
- Subsequent builds will be faster due to incremental compilation
- For development, use debug builds for faster compilation
- Use release builds for performance testing and distribution

## Support

For build issues or questions:
1. Check the project's GitHub issues
2. Verify all prerequisites are correctly installed
3. Ensure submodules are properly initialized
4. Review Android SDK installation and environment variables