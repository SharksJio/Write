package com.jio.writingapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;

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
import androidx.core.content.FileProvider;
import android.content.ClipboardManager;
import android.content.ClipDescription;
import android.provider.Settings;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.view.ViewGroup;
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

public class NativeActivity extends AppCompatActivity implements View.OnTouchListener, View.OnHoverListener
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
  
  // UI Components
  private NativeCanvasView mCanvasView;
  private LinearLayout mVerticalToolbar;
  private MaterialButtonToggleGroup mToolToggleGroup;
  private FrameLayout mMainContainer;
  private CoordinatorLayout mRootLayout;
  
  static {
    System.loadLibrary("main");
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    // Initialize native code
    jniOnCreate();
    
    // Setup Material 3 UI layout
    setupMaterial3UI();
    
    // Handle intent
    Intent intent = getIntent();
    if (intent != null) {
      handleIntent(intent);
    }
  }
  
  private void setupMaterial3UI() {
    // Use the Material 3 XML layout
    setContentView(R.layout.activity_main);
    
    // Get references to layout components
    mRootLayout = findViewById(R.id.root_layout);
    mVerticalToolbar = findViewById(R.id.vertical_toolbar);
    mToolToggleGroup = findViewById(R.id.tool_toggle_group);
    mMainContainer = findViewById(R.id.main_container);
    mCanvasView = findViewById(R.id.canvas_view);
    
    // Set up touch listeners
    mCanvasView.setOnTouchListener(this);
    mCanvasView.setOnHoverListener(this);
    
    // Setup tool selection listeners
    setupToolListeners();
  }
  
  private void setupToolListeners() {
    mToolToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
      if (isChecked) {
        // Handle tool selection
        if (checkedId == R.id.tool_pen) {
          // Switch to pen tool
          setDrawingTool(0);
        } else if (checkedId == R.id.tool_eraser) {
          // Switch to eraser tool
          setDrawingTool(1);
        } else if (checkedId == R.id.tool_select) {
          // Switch to select tool
          setDrawingTool(2);
        } else if (checkedId == R.id.tool_insert_space) {
          // Switch to insert space tool
          setDrawingTool(3);
        }
      }
    });
  }
  
  private void setDrawingTool(int tool) {
    // Send tool change to native code
    jniSendKeyEvent(tool + 1000, 1); // Custom key codes for tools
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

  private void handleIntent(Intent intent) {
    String action = intent.getAction();
    String data = intent.getDataString();
    String type = intent.getType();
    
    jniSetIntent(action != null ? action : "", 
                 data != null ? data : "", 
                 type != null ? type : "");
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
    return true;
  }

  // Additional methods for image insertion, file handling, etc.
  // (These would be similar to the original implementation but cleaned up)
  
  private String packageId() {
    return getApplicationContext().getPackageName();
  }
  
  public void openUrl(String url) {
    Intent viewUrlIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
    startActivity(viewUrlIntent);
  }
}