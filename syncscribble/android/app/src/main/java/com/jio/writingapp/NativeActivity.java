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
import android.view.Menu;
import android.view.MenuItem;

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
  
  // AI Agent
  private AIAgentManager aiAgent;
  
  static {
    System.loadLibrary("main");
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    // Initialize native code
    jniOnCreate();
    
    // Initialize AI Agent
    aiAgent = AIAgentManager.getInstance(this);
    
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
  
  private String packageId() {
    return getApplicationContext().getPackageName();
  }
  
  public void openUrl(String url) {
    Intent viewUrlIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
    startActivity(viewUrlIntent);
  }
  
  // Image insertion functionality
  public void combinedGetImage() {
    final File cameraFile = new File(getExternalCacheDir(), "_camera.jpg");
    cameraFile.delete(); // remove existing
    String authority = packageId() + ".fileprovider";
    Uri outputFileUri = FileProvider.getUriForFile(this, authority, cameraFile);
    
    // capture image (camera) intents
    final java.util.List<Intent> cameraIntents = new java.util.ArrayList<Intent>();
    final Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    final java.util.List<ResolveInfo> listCam = getPackageManager().queryIntentActivities(captureIntent, 0);
    for(ResolveInfo res : listCam) {
      final Intent intent = new Intent(captureIntent);
      intent.setComponent(new android.content.ComponentName(res.activityInfo.packageName, res.activityInfo.name));
      intent.setPackage(res.activityInfo.packageName);
      intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
      cameraIntents.add(intent);
    }

    // select image intents
    final Intent galleryIntent = new Intent();
    galleryIntent.setType("image/*");
    galleryIntent.setAction(Intent.ACTION_PICK);

    // combined intent
    final Intent chooserIntent = Intent.createChooser(galleryIntent, "Select Image");
    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new android.os.Parcelable[]{}));
    startActivityForResult(chooserIntent, 1022);
  }
  
  private boolean doInsertImage(InputStream inputStream, boolean fromintent) {
    try {
      BitmapFactory.Options opt = new BitmapFactory.Options();
      opt.inPreferredConfig = Bitmap.Config.ARGB_8888;
      opt.inSampleSize = 1;
      Bitmap img = null;
      do {
        try {
          img = BitmapFactory.decodeStream(inputStream, null, opt);
        } catch(OutOfMemoryError e) {}
        opt.inSampleSize *= 2;
      } while(img == null && opt.inSampleSize <= 16);
      
      if(img != null && img.getWidth() > 0 && img.getHeight() > 0) {
        jniInsertImage(img, opt.outMimeType, fromintent);
        return true;
      } else {
        Toast.makeText(this, "Error opening image", Toast.LENGTH_SHORT).show();
        return false;
      }
    } catch(Exception e) {
      Log.v("doInsertImage", "Exception decoding image: ", e);
      Toast.makeText(this, "Error opening image", Toast.LENGTH_SHORT).show();
      return false;
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    
    if(requestCode == 1022 && resultCode == RESULT_OK) {
      final File cameraFile = new File(getExternalCacheDir(), "_camera.jpg");
      if(cameraFile.length() > 0) {
        try {
          doInsertImage(new FileInputStream(cameraFile), false);
        } catch(FileNotFoundException e) {
          Toast.makeText(this, "Camera image not found", Toast.LENGTH_SHORT).show();
        }
      } else if(intent != null && intent.getData() != null) {
        try {
          doInsertImage(getContentResolver().openInputStream(intent.getData()), false);
        } catch(FileNotFoundException e) {
          Toast.makeText(this, "Selected image not found", Toast.LENGTH_SHORT).show();
        }
      }
    }
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    String action = intent.getAction();
    
    if(Intent.ACTION_SEND.equals(action)) {
      if(intent.getType() != null && intent.getType().startsWith("image/")) {
        final Uri imageURI = (Uri)intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if(imageURI != null) {
          try {
            if(doInsertImage(getContentResolver().openInputStream(imageURI), true)) {
              Toast.makeText(this, "Image copied to clipboard. Paste where desired.", Toast.LENGTH_SHORT).show();
            }
          } catch(FileNotFoundException e) {
            Toast.makeText(this, "Shared image not found", Toast.LENGTH_SHORT).show();
          }
        }
      }
    } else if(Intent.ACTION_VIEW.equals(action) || Intent.ACTION_EDIT.equals(action)) {
      if(("text/html".equals(intent.getType()) || "image/svg+xml".equals(intent.getType()))
          && intent.getData() != null) {
        if(intent.getData().toString().startsWith("content://")) {
          try {
            ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(intent.getData(), "r");
            jniOpenFileDesc(intent.getData().getPath(), pfd.getFd());
            pfd.close();
          } catch(Exception e) {
            Log.v("onNewIntent", "Error opening document: " + intent.getData().toString(), e);
          }
        } else {
          jniOpenFile(intent.getData().getPath());
        }
      }
    }
    
    handleIntent(intent);
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.main_menu, menu);
    return true;
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    
    if (id == R.id.action_ai_chat) {
      openAIChat();
      return true;
    } else if (id == R.id.action_ai_config) {
      openAIConfig();
      return true;
    }
    
    return super.onOptionsItemSelected(item);
  }
  
  private void openAIChat() {
    Intent intent = new Intent(this, AIChatActivity.class);
    startActivity(intent);
  }
  
  private void openAIConfig() {
    Intent intent = new Intent(this, AIConfigActivity.class);
    startActivity(intent);
  }
}