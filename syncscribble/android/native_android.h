#pragma once

#ifdef ANDROID_NATIVE_UI

#include <jni.h>

// Native Android UI equivalents for SDL functions
// These functions provide the same interface as SDL functions but use native Android APIs

// Equivalent to SDL_AndroidGetJNIEnv()
JNIEnv* Native_AndroidGetJNIEnv();

// Equivalent to SDL_AndroidGetActivity()
jobject Native_AndroidGetActivity();

// Equivalent to SDL_AndroidGetExternalStoragePath()
const char* Native_AndroidGetExternalStoragePath();

#endif // ANDROID_NATIVE_UI