# Android Native UI Implementation

This document describes the new Android native UI implementation for Write that replaces the SDL-based UI.

## Overview

The Write app has been modified to support two Android build modes:

1. **SDL Mode (traditional)** - Uses SDL for windowing and input (default)
2. **Native UI Mode** - Uses native Android UI components

## Architecture

### Native UI Mode Components

#### Java Layer
- `NativeActivity.java` - Main activity that replaces `MainActivity`/`SDLActivity`
- `NativeCanvasView.java` - Custom `SurfaceView` for drawing canvas
- `activity_main.xml` - Android layout that mirrors the SVG UI structure

#### Native Layer
- `native_bridge.cpp` - JNI bridge connecting Android UI to C++ core
- Modified `application.cpp` - Conditional entry point for native mode
- Updated `Makefile` - Build system support for `ANDROID_NATIVE_UI` mode

## Key Differences from SDL Mode

### UI Framework
- **SDL Mode**: Everything rendered through OpenGL surface managed by SDL
- **Native Mode**: Uses Android's native view hierarchy and layout system

### Input Handling
- **SDL Mode**: SDL processes all touch/mouse events and converts to app events
- **Native Mode**: Android's native touch handling directly interfaces with C++ core

### Window Management
- **SDL Mode**: SDL manages the entire window/surface
- **Native Mode**: Android's Activity/View system manages UI, custom SurfaceView for drawing

## Building

### Native UI Mode
```bash
# Set environment variable to enable native UI mode
export ANDROID_NATIVE_UI=1

# Build the app
cd syncscribble/android
./gww assembleRelease
```

### SDL Mode (traditional)
```bash
# Don't set ANDROID_NATIVE_UI or set it to 0
cd syncscribble/android
./gww assembleRelease
```

## Implementation Details

### Layout Structure
The native UI mirrors the original SVG-based layout:

```
LinearLayout (main)
├── LinearLayout (main_toolbar_container)
├── LinearLayout (pen_toolbar_container) 
├── FrameLayout (main_container)
│   └── NativeCanvasView (canvas_view)
└── LinearLayout (notify_toolbar_container)
```

### Touch Event Flow
1. Android touch events → `NativeActivity.onTouch()`
2. JNI call → `jniSendTouchEvent()`
3. Create `InputEvent` → Send to `ScribbleArea.doInputEvent()`
4. C++ core processes input as normal

### Drawing Integration
1. `NativeCanvasView` provides drawing surface
2. JNI callbacks integrate with existing `Application::layoutAndDraw()`
3. Maintains compatibility with SVG GUI system

## Benefits

1. **Removes SDL Dependency** - Eliminates ~3MB SDL library
2. **Native Android Feel** - Better integration with Android system
3. **Easier Maintenance** - Uses standard Android development patterns
4. **Better Performance** - Direct Android input handling, less translation layers
5. **Smaller APK Size** - No SDL libraries to bundle

## Compatibility

- Maintains full compatibility with existing C++ core
- All drawing, input processing, and app logic unchanged
- Pen/stylus support preserved
- File handling and intents work the same

## Testing

The implementation preserves all existing functionality:
- Touch and stylus input
- Drawing and note-taking features  
- File operations
- Settings and configuration
- All existing app features

## Future Enhancements

With native Android UI foundation in place, future improvements could include:
- Native Android toolbars with Material Design
- Better integration with Android system UI
- Native Android dialogs and menus
- Improved accessibility support
- Better landscape/portrait handling