#!/bin/bash
# Automated build script for Write Android APK
# This script handles the complete build process from setup to APK generation

set -e  # Exit on any error

echo "Write Android APK Build Script"
echo "=============================="

# Configuration
ANDROID_SDK_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
REQUIRED_JAVA_VERSION="17"
BUILD_TYPE="${1:-debug}"  # Default to debug build

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Helper functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

check_command() {
    if ! command -v $1 &> /dev/null; then
        log_error "$1 is not installed or not in PATH"
        return 1
    fi
    return 0
}

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."
    
    # Check Java
    if check_command java; then
        JAVA_VERSION=$(java -version 2>&1 | head -n1 | cut -d'"' -f2 | cut -d'.' -f1)
        if [ "$JAVA_VERSION" -ge "$REQUIRED_JAVA_VERSION" ]; then
            log_info "Java $JAVA_VERSION detected - OK"
        else
            log_error "Java $REQUIRED_JAVA_VERSION or later required, found $JAVA_VERSION"
            return 1
        fi
    else
        log_error "Java not found. Please install OpenJDK $REQUIRED_JAVA_VERSION or later"
        return 1
    fi
    
    # Check git
    if ! check_command git; then
        log_error "Git not found. Please install git"
        return 1
    fi
    
    # Check curl
    if ! check_command curl; then
        log_error "curl not found. Please install curl"
        return 1
    fi
    
    # Check unzip
    if ! check_command unzip; then
        log_error "unzip not found. Please install unzip"
        return 1
    fi
    
    log_info "All prerequisites satisfied"
}

# Install Android SDK
install_android_sdk() {
    if [ -d "$HOME/android-sdk" ] && [ -f "$HOME/android-sdk/cmdline-tools/bin/sdkmanager" ]; then
        log_info "Android SDK already installed at $HOME/android-sdk"
        return 0
    fi
    
    log_info "Installing Android SDK..."
    
    mkdir -p $HOME/android-sdk
    cd $HOME/android-sdk
    
    log_info "Downloading Android SDK command line tools..."
    curl -o cmdline-tools.zip "$ANDROID_SDK_URL"
    
    log_info "Extracting command line tools..."
    unzip -q cmdline-tools.zip
    
    log_info "Installing SDK components..."
    echo "y" | cmdline-tools/bin/sdkmanager --sdk_root=. --install \
        'build-tools;34.0.0' \
        'platforms;android-34' \
        'platform-tools' \
        'ndk;26.3.11579264' \
        'cmake;3.18.1'
    
    log_info "Android SDK installation complete"
}

# Setup environment
setup_environment() {
    log_info "Setting up build environment..."
    
    export ANDROID_HOME="$HOME/android-sdk"
    echo "ANDROID_HOME set to: $ANDROID_HOME"
    
    # Add to PATH
    export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/bin:$PATH"
}

# Initialize git submodules
init_submodules() {
    log_info "Initializing git submodules..."
    
    if [ ! -f ".gitmodules" ]; then
        log_error "Not in a Git repository or .gitmodules not found"
        return 1
    fi
    
    git submodule update --init --recursive
    
    # Ensure SDL is on the correct branch
    if [ -d "SDL" ]; then
        cd SDL
        if git branch -r | grep -q "origin/write-android"; then
            git checkout write-android
            log_info "SDL switched to write-android branch"
        else
            log_warn "write-android branch not found in SDL, using default"
        fi
        cd ..
    fi
}

# Build APK
build_apk() {
    log_info "Building Android APK (${BUILD_TYPE})..."
    
    cd syncscribble/android
    
    # Make sure gww script is executable
    chmod +x gww
    
    # Setup gradle wrapper if needed
    if [ ! -f "gradlew" ]; then
        log_info "Setting up Gradle wrapper..."
        ./gww --version > /dev/null
    fi
    
    # Build the APK
    case "$BUILD_TYPE" in
        "debug")
            log_info "Building debug APK..."
            ./gww assembleDebug
            APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
            ;;
        "release")
            log_info "Building release APK..."
            ./gww assembleRelease
            APK_PATH="app/build/outputs/apk/release/app-release.apk"
            ;;
        "arm64rel")
            log_info "Building ARM64 release APK..."
            ./gww assembleArm64rel
            APK_PATH="app/build/outputs/apk/arm64rel/app-arm64rel.apk"
            ;;
        *)
            log_error "Unknown build type: $BUILD_TYPE"
            log_error "Supported types: debug, release, arm64rel"
            return 1
            ;;
    esac
    
    # Check if build was successful
    if [ -f "$APK_PATH" ]; then
        APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
        log_info "Build successful! APK created at: $APK_PATH (${APK_SIZE})"
        
        # Show APK info
        log_info "APK Information:"
        aapt dump badging "$APK_PATH" | grep -E "(package|sdkVersion|targetSdkVersion|application-label)"
        
        return 0
    else
        log_error "Build failed - APK not found at expected location: $APK_PATH"
        return 1
    fi
}

# Main execution
main() {
    echo "Build type: $BUILD_TYPE"
    echo ""
    
    # Change to script directory
    SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    cd "$SCRIPT_DIR"
    
    # Execute build steps
    check_prerequisites || exit 1
    install_android_sdk || exit 1
    setup_environment || exit 1
    init_submodules || exit 1
    build_apk || exit 1
    
    log_info "Build process completed successfully!"
    log_info "You can now install the APK using: adb install $APK_PATH"
}

# Show help
show_help() {
    echo "Usage: $0 [BUILD_TYPE]"
    echo ""
    echo "BUILD_TYPE options:"
    echo "  debug     - Debug build (default)"
    echo "  release   - Release build"
    echo "  arm64rel  - ARM64-only release build"
    echo ""
    echo "Examples:"
    echo "  $0              # Build debug APK"
    echo "  $0 release      # Build release APK"
    echo "  $0 arm64rel     # Build ARM64 release APK"
    echo ""
    echo "Prerequisites:"
    echo "  - Java 17 or later"
    echo "  - Git"
    echo "  - curl"
    echo "  - unzip"
    echo "  - Internet connection (for initial setup)"
}

# Handle command line arguments
case "$1" in
    "-h"|"--help"|"help")
        show_help
        exit 0
        ;;
    *)
        main
        ;;
esac