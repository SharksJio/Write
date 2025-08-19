#include <jni.h>
#include <android/log.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <string.h>
#include "application.h"
#include "scribbleapp.h"
#include "scribbleinput.h"
#include "basics.h"

// Forward declaration
class ScribbleApp;
extern ScribbleApp* scribbleApp;

#define LOG_TAG "WriteNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Global variables for the native Android implementation
static JavaVM* g_jvm = nullptr;
static jobject g_activity = nullptr;
static ANativeWindow* g_native_window = nullptr;
static ScribbleApp* g_scribble_app = nullptr;

// JNI function declarations
extern "C" {

// Activity lifecycle
JNIEXPORT void JNICALL
Java_com_styluslabs_writeqt_NativeActivity_jniOnCreate(JNIEnv *env, jobject thiz);

JNIEXPORT void JNICALL
Java_com_styluslabs_writeqt_NativeActivity_jniOnStart(JNIEnv *env, jobject thiz);

JNIEXPORT void JNICALL
Java_com_styluslabs_writeqt_NativeActivity_jniOnResume(JNIEnv *env, jobject thiz);

JNIEXPORT void JNICALL
Java_com_styluslabs_writeqt_NativeActivity_jniOnPause(JNIEnv *env, jobject thiz);

JNIEXPORT void JNICALL
Java_com_styluslabs_writeqt_NativeActivity_jniOnStop(JNIEnv *env, jobject thiz);

JNIEXPORT void JNICALL
Java_com_styluslabs_writeqt_NativeActivity_jniOnDestroy(JNIEnv *env, jobject thiz);

// Surface handling
JNIEXPORT void JNICALL
Java_com_styluslabs_writeqt_NativeCanvasView_jniSurfaceCreated(JNIEnv *env, jobject thiz, jobject surface, jint width, jint height);

JNIEXPORT void JNICALL
Java_com_styluslabs_writeqt_NativeCanvasView_jniSurfaceChanged(JNIEnv *env, jobject thiz, jobject surface, jint width, jint height);

JNIEXPORT void JNICALL
Java_com_styluslabs_writeqt_NativeCanvasView_jniSurfaceDestroyed(JNIEnv *env, jobject thiz);

// Input handling
JNIEXPORT void JNICALL
Java_com_styluslabs_writeqt_NativeActivity_jniSendTouchEvent(JNIEnv *env, jobject thiz, jint action, jint pointerId, jfloat x, jfloat y, jfloat pressure);

JNIEXPORT void JNICALL
Java_com_styluslabs_writeqt_NativeActivity_jniSendKeyEvent(JNIEnv *env, jobject thiz, jint keyCode, jint action);

// Intent handling
JNIEXPORT void JNICALL
Java_com_styluslabs_writeqt_NativeActivity_jniSetIntent(JNIEnv *env, jobject thiz, jstring action, jstring data, jstring type);

// Drawing
JNIEXPORT void JNICALL
Java_com_styluslabs_writeqt_NativeCanvasView_jniDrawFrame(JNIEnv *env, jobject thiz);

} // extern "C"

// Helper function to initialize the native application
static void initializeNativeApp() {
    if (!g_scribble_app) {
        // Initialize the core application using our native entry point
        extern int android_native_main(int argc, char* argv[]);
        
        char* dummy_argv[] = { (char*)"write", nullptr };
        android_native_main(1, dummy_argv);
        
        // Get the initialized ScribbleApp instance
        extern ScribbleApp* scribbleApp;
        g_scribble_app = scribbleApp;
        
        LOGI("Native application initialized");
    }
}

// Activity lifecycle implementations
JNIEXPORT void JNICALL
Java_com_styluslabs_writeqt_NativeActivity_jniOnCreate(JNIEnv *env, jobject thiz) {
    LOGI("jniOnCreate called");
    
    // Store JVM and activity reference
    env->GetJavaVM(&g_jvm);
    g_activity = env->NewGlobalRef(thiz);
    
    // Initialize native application
    initializeNativeApp();
}

JNIEXPORT void JNICALL
Java_com_styluslabs_writeqt_NativeActivity_jniOnStart(JNIEnv *env, jobject thiz) {
    LOGI("jniOnStart called");
    // Handle app start
}

JNIEXPORT void JNICALL
Java_com_styluslabs_writeqt_NativeActivity_jniOnResume(JNIEnv *env, jobject thiz) {
    LOGI("jniOnResume called");
    Application::isSuspended = false;
}

JNIEXPORT void JNICALL
Java_com_styluslabs_writeqt_NativeActivity_jniOnPause(JNIEnv *env, jobject thiz) {
    LOGI("jniOnPause called");
    Application::isSuspended = true;
}

JNIEXPORT void JNICALL
Java_com_styluslabs_writeqt_NativeActivity_jniOnStop(JNIEnv *env, jobject thiz) {
    LOGI("jniOnStop called");
    // Handle app stop
}

JNIEXPORT void JNICALL
Java_com_styluslabs_writeqt_NativeActivity_jniOnDestroy(JNIEnv *env, jobject thiz) {
    LOGI("jniOnDestroy called");
    
    // Cleanup
    if (g_native_window) {
        ANativeWindow_release(g_native_window);
        g_native_window = nullptr;
    }
    
    if (g_activity) {
        env->DeleteGlobalRef(g_activity);
        g_activity = nullptr;
    }
    
    if (g_scribble_app) {
        delete g_scribble_app;
        g_scribble_app = nullptr;
    }
}

// Surface handling implementations
JNIEXPORT void JNICALL
Java_com_styluslabs_writeqt_NativeCanvasView_jniSurfaceCreated(JNIEnv *env, jobject thiz, jobject surface, jint width, jint height) {
    LOGI("jniSurfaceCreated: %dx%d", width, height);
    
    // Get native window from surface
    g_native_window = ANativeWindow_fromSurface(env, surface);
    if (!g_native_window) {
        LOGE("Failed to get native window from surface");
        return;
    }
    
    // Set up the rendering context
    if (g_scribble_app) {
        // Initialize rendering for the given surface size
        // This replaces SDL window setup
        LOGI("Setting up rendering context for %dx%d", width, height);
    }
}

JNIEXPORT void JNICALL
Java_com_styluslabs_writeqt_NativeCanvasView_jniSurfaceChanged(JNIEnv *env, jobject thiz, jobject surface, jint width, jint height) {
    LOGI("jniSurfaceChanged: %dx%d", width, height);
    
    if (g_scribble_app && g_native_window) {
        // Handle surface size change
        ANativeWindow_setBuffersGeometry(g_native_window, width, height, WINDOW_FORMAT_RGBA_8888);
    }
}

JNIEXPORT void JNICALL
Java_com_styluslabs_writeqt_NativeCanvasView_jniSurfaceDestroyed(JNIEnv *env, jobject thiz) {
    LOGI("jniSurfaceDestroyed called");
    
    if (g_native_window) {
        ANativeWindow_release(g_native_window);
        g_native_window = nullptr;
    }
}

// Input handling implementations
JNIEXPORT void JNICALL
Java_com_styluslabs_writeqt_NativeActivity_jniSendTouchEvent(JNIEnv *env, jobject thiz, jint action, jint pointerId, jfloat x, jfloat y, jfloat pressure) {
    if (!g_scribble_app) return;
    
    // Create InputEvent and send to ScribbleInput
    // This replaces SDL touch event handling
    inputsource_t inputSource = INPUTSOURCE_TOUCH;
    
    // Determine input type based on action
    inputevent_t eventType = INPUTEVENT_NONE;
    switch (action) {
        case 0: eventType = INPUTEVENT_DOWN; break;    // Touch down
        case 1: eventType = INPUTEVENT_MOVE; break;    // Touch move  
        case 2: eventType = INPUTEVENT_UP; break;      // Touch up
        case 3: eventType = INPUTEVENT_CANCEL; break;  // Touch cancel
        case 4: eventType = INPUTEVENT_HOVER; break;   // Hover enter
        case 5: eventType = INPUTEVENT_HOVER; break;   // Hover move
        case 6: eventType = INPUTEVENT_HOVER; break;   // Hover exit
    }
    
    // Create and send input event
    InputEvent ievent(inputSource, MODEMOD_NONE, 0, 1.0); // timestamp=0, maxwidth=1.0
    ievent.points.push_back(InputPoint(eventType, x, y, pressure > 0 ? pressure : 1.0f));
    
    // Send to the current scribble input handler
    // Note: This is a simplified version - the actual implementation would need
    // to route this through the proper ScribbleInput instance
    LOGI("Touch event: action=%d, id=%d, pos=(%.2f,%.2f), pressure=%.2f", action, pointerId, x, y, pressure);
}

JNIEXPORT void JNICALL
Java_com_styluslabs_writeqt_NativeActivity_jniSendKeyEvent(JNIEnv *env, jobject thiz, jint keyCode, jint action) {
    if (!g_scribble_app) return;
    
    LOGI("Key event: code=%d, action=%d", keyCode, action);
    // Handle key events if needed
}

// Intent handling implementation
JNIEXPORT void JNICALL
Java_com_styluslabs_writeqt_NativeActivity_jniSetIntent(JNIEnv *env, jobject thiz, jstring action, jstring data, jstring type) {
    const char* actionStr = env->GetStringUTFChars(action, nullptr);
    const char* dataStr = env->GetStringUTFChars(data, nullptr);
    const char* typeStr = env->GetStringUTFChars(type, nullptr);
    
    LOGI("Intent: action=%s, data=%s, type=%s", actionStr, dataStr, typeStr);
    
    // Handle intent in ScribbleApp
    if (g_scribble_app) {
        // Process the intent data
    }
    
    env->ReleaseStringUTFChars(action, actionStr);
    env->ReleaseStringUTFChars(data, dataStr);
    env->ReleaseStringUTFChars(type, typeStr);
}

// Drawing implementation
JNIEXPORT void JNICALL
Java_com_styluslabs_writeqt_NativeCanvasView_jniDrawFrame(JNIEnv *env, jobject thiz) {
    if (!g_scribble_app || !g_native_window) return;
    
    // Trigger a drawing update
    // This replaces the SDL render loop
    ANativeWindow_Buffer buffer;
    if (ANativeWindow_lock(g_native_window, &buffer, nullptr) == 0) {
        // Clear buffer
        memset(buffer.bits, 255, buffer.stride * buffer.height * 4); // White background
        
        // Perform actual drawing through the application
        // This would integrate with the existing layoutAndDraw system
        if (Application::gui) {
            // Application::layoutAndDraw(); // This would need adaptation for native window
        }
        
        ANativeWindow_unlockAndPost(g_native_window);
    }
}

// JNI Library loading
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    LOGI("JNI_OnLoad called");
    g_jvm = vm;
    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* vm, void* reserved) {
    LOGI("JNI_OnUnload called");
    g_jvm = nullptr;
}