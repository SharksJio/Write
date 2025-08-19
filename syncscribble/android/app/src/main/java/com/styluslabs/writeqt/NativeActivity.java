package com.styluslabs.writeqt;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import android.view.KeyEvent;
import android.view.Surface;
import android.graphics.Point;
import android.content.pm.ResolveInfo;
import android.media.AudioManager;
import android.widget.Toast;
import android.util.Log;
import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.content.FileProvider;
import android.content.ClipboardManager;
import android.content.ClipDescription;
import android.provider.Settings;
import android.widget.LinearLayout;
import android.widget.Toolbar;
import android.widget.FrameLayout;
import android.view.ViewGroup;

// for onTouch
import android.view.View;
import android.view.MotionEvent;
import android.view.InputDevice;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

public class NativeActivity extends Activity implements View.OnTouchListener, View.OnHoverListener
{
  private static native void jniInsertImage(Bitmap bitmap, String mimetype, boolean fromintent);
  private static native void jniOpenFile(String filename);
  private static native void jniOpenFileDesc(String filename, int fd);
  private static native void jniSetIntent(String action, String data, String type);
  private static native void jniOnCreate();
  private static native void jniOnStart();
  private static native void jniOnResume();
  private static native void jniOnPause();
  private static native void jniOnStop();
  private static native void jniOnDestroy();
  private static native void jniOnSaveInstanceState();
  private static native void jniOnLowMemory();
  
  // Touch event handling
  private static native void jniSendTouchEvent(int action, int pointerId, float x, float y, float pressure);
  private static native void jniSendKeyEvent(int keyCode, int action);
  
  // Canvas drawing surface
  private NativeCanvasView mCanvasView;
  private LinearLayout mMainLayout;
  private LinearLayout mToolbarContainer;
  private LinearLayout mPenToolbarContainer;
  private FrameLayout mMainContainer;
  
  static {
    System.loadLibrary("main");
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    // Initialize native code
    jniOnCreate();
    
    // Setup UI layout
    setupUI();
    
    // Handle intent
    Intent intent = getIntent();
    if (intent != null) {
      handleIntent(intent);
    }
  }
  
  private void setupUI() {
    // Use the XML layout instead of creating UI programmatically
    setContentView(R.layout.activity_main);
    
    // Get references to layout components
    mToolbarContainer = findViewById(R.id.main_toolbar_container);
    mPenToolbarContainer = findViewById(R.id.pen_toolbar_container);
    mMainContainer = findViewById(R.id.main_container);
    mCanvasView = findViewById(R.id.canvas_view);
    
    // Set up touch listeners
    mCanvasView.setOnTouchListener(this);
    mCanvasView.setOnHoverListener(this);
  }

  @Override
  protected void onStart() {
    super.onStart();
    jniOnStart();
  }

  @Override
  protected void onResume() {
    super.onResume();
    jniOnResume();
  }

  @Override
  protected void onPause() {
    super.onPause();
    jniOnPause();
  }

  @Override
  protected void onStop() {
    super.onStop();
    jniOnStop();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    jniOnDestroy();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    jniOnSaveInstanceState();
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    jniOnLowMemory();
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    if (intent != null) {
      handleIntent(intent);
    }
  }

  private void handleIntent(Intent intent) {
    String action = intent.getAction();
    String data = intent.getDataString();
    String type = intent.getType();
    
    jniSetIntent(action != null ? action : "", data != null ? data : "", type != null ? type : "");
    
    // Handle image insertion
    if (Intent.ACTION_SEND.equals(action) && type != null && type.startsWith("image/")) {
      Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
      if (imageUri != null) {
        try {
          InputStream inputStream = getContentResolver().openInputStream(imageUri);
          Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
          if (bitmap != null) {
            jniInsertImage(bitmap, type, true);
          }
        } catch (Exception e) {
          Log.e("NativeActivity", "Error handling image intent", e);
        }
      }
    }
  }

  // Touch Events
  @Override
  public boolean onTouch(View v, MotionEvent event) {
    int action = event.getActionMasked();
    int pointerCount = event.getPointerCount();
    
    for (int i = 0; i < pointerCount; i++) {
      int pointerId = event.getPointerId(i);
      float x = event.getX(i);
      float y = event.getY(i);
      float pressure = event.getPressure(i);
      
      // Map MotionEvent actions to our native handling
      int nativeAction = 0;
      switch (action) {
        case MotionEvent.ACTION_DOWN:
        case MotionEvent.ACTION_POINTER_DOWN:
          nativeAction = 0; // Touch down
          break;
        case MotionEvent.ACTION_MOVE:
          nativeAction = 1; // Touch move
          break;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_POINTER_UP:
          nativeAction = 2; // Touch up
          break;
        case MotionEvent.ACTION_CANCEL:
          nativeAction = 3; // Touch cancel
          break;
      }
      
      jniSendTouchEvent(nativeAction, pointerId, x, y, pressure);
    }
    
    return true;
  }

  @Override
  public boolean onHover(View v, MotionEvent event) {
    // Handle hover events for stylus
    float x = event.getX();
    float y = event.getY();
    float pressure = event.getPressure();
    
    int action = event.getActionMasked();
    int nativeAction = 0;
    switch (action) {
      case MotionEvent.ACTION_HOVER_ENTER:
        nativeAction = 4; // Hover enter
        break;
      case MotionEvent.ACTION_HOVER_MOVE:
        nativeAction = 5; // Hover move
        break;
      case MotionEvent.ACTION_HOVER_EXIT:
        nativeAction = 6; // Hover exit
        break;
    }
    
    jniSendTouchEvent(nativeAction, 0, x, y, pressure);
    return true;
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    jniSendKeyEvent(keyCode, 0); // Key down
    return super.onKeyDown(keyCode, event);
  }

  @Override
  public boolean onKeyUp(int keyCode, KeyEvent event) {
    jniSendKeyEvent(keyCode, 1); // Key up
    return super.onKeyUp(keyCode, event);
  }

  // Methods that can be called from native code
  public static void showToast(String message) {
    // This method can be called from JNI to show toast messages
  }
  
  public static Context getContext() {
    // Return application context for native code
    return null; // TODO: implement properly
  }
}